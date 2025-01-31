package com.jf.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.github.pagehelper.PageInfo;
import com.jf.annotation.AuthPassport;
import com.jf.common.BaseController;
import com.jf.controller.view.ViewExcel;
import com.jf.convert.Convert;
import com.jf.database.model.manage.*;
import com.jf.date.DateUtil;
import com.jf.entity.ResMsg;
import com.jf.entity.Tree;
import com.jf.database.enums.ResCode;
import com.jf.file.Directory;
import com.jf.file.FileUtil;
import com.jf.http.HttpUtil;
import com.jf.obj.BeanUtil;
import com.jf.service.system.AddrService;
import com.jf.service.system.AdminService;
import com.jf.service.system.ModuleService;
import com.jf.service.system.SystemService;
import com.jf.string.StringUtil;
import com.jf.system.conf.IConstant;
import com.jf.system.conf.SysConfig;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统管理模块
 *
 * @author rick
 * @version 2.0
 */
@Controller
@RequestMapping("/admin/system")
public class SystemBackController extends BaseController {

    @Resource
    private AdminService adminService;
    @Resource
    private ModuleService moduleService;
    @Resource
    private SystemService systemService;
    @Resource
    private AddrService addrService;
    @Resource
    private SysConfig config;

    @RequestMapping("file")
    @AuthPassport
    public String file() {
        return "system/file";
    }

    @RequestMapping("icons")
    @AuthPassport(right = false)
    public String icons() {
        return "system/icons";
    }

    @RequestMapping("/tools")
    @AuthPassport(right = false)
    public String formBuilder() {
        return "system/tools";
    }

    @RequestMapping("/jenkins")
    @AuthPassport(right = false)
    public String jenkins(HttpServletRequest request) {
        Admin admin = getSession(request, IConstant.SESSION_ADMIN);
        if (admin == null || admin.getRole().getRoleFlag() != 0) {
            return "error/refuse";
        }
        return "system/jenkins";
    }

    /**
     * jenkins自动化部署
     *
     * @param ip
     * @param type
     * @param auth
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/jenkins_opt", method = RequestMethod.POST)
    @ResponseBody
    @AuthPassport(right = false)
    public ResMsg jenkins(String ip, String type, String auth, HttpServletRequest request) throws Exception {
        if (StringUtil.isBlank(ip) || StringUtil.isBlank(type) || StringUtil.isBlank(auth)) {
            return new ResMsg(1, "invalid param");
        }
        Admin admin = getSession(request, IConstant.SESSION_ADMIN);
        if (admin == null || admin.getRole().getRoleFlag() != 0) {
            return new ResMsg(1, "refuse if superadmin");
        }

        String json = "";
        if ("post".equals(type)) {
            json = HttpUtil.postWithAuthorization(ip, null, auth);
        } else if ("get".equals(type)) {
            json = HttpUtil.getWithAuthorization(ip, auth);
        }
        if (json != null && json.startsWith("<html>")) {
            return new ResMsg(ResCode.REFUSE.code(), ResCode.REFUSE.msg(), json);
        }
        if (HttpUtil.STR_ERROR.equals(json)) {
            return new ResMsg(ResCode.ERROR.code(), ResCode.ERROR.msg());
        }
        if (HttpUtil.STR_TIMEOUT.equals(json)) {
            return new ResMsg(ResCode.TIMEOUT.code(), ResCode.TIMEOUT.msg());
        }
        return new ResMsg(ResCode.SUCCESS.code(), ResCode.SUCCESS.msg(), json);
    }

    /**
     * 管理员列表
     *
     * @param condition
     * @param map
     * @return
     */
    @RequestMapping("/adminList")
    @AuthPassport
    public String adminList(Admin condition, ModelMap map) {
        PageInfo pageInfo = adminService.findAdminByPage(condition);
        map.addAttribute("pageInfo", pageInfo);
        map.addAllAttributes(BeanUtil.beanToMap(condition));

        List<Role> roles = moduleService.findRoleList();
        map.addAttribute("roles", roles);
        return "system/admin_list";
    }

    /**
     * 管理员详细
     *
     * @param adminId
     * @param map
     * @return
     */
    @RequestMapping("/adminDetail")
    @AuthPassport(right = false)
    public String adminDetail(Integer adminId, ModelMap map) {
        List<Role> roles = moduleService.findRoleList();
        map.addAttribute("roles", roles);
        if (adminId == null) {
            map.addAttribute("add", true);
        } else {
            Admin admin = adminService.findAdminById(adminId);
            map.addAttribute("adm", admin);
        }
        return "system/admin_edit";
    }

    /**
     * 启用和禁用管理员
     *
     * @param adminId
     * @return
     */
    @RequestMapping("/adminEnable")
    @AuthPassport
    @ResponseBody
    public ResMsg adminEnable(Integer adminId, HttpServletRequest request) {
        if (adminId == null) {
            return new ResMsg(ResCode.CODE_22.code(), ResCode.CODE_22.msg());
        }
        if (adminService.deleteAdmin(adminId) > 0) {
            systemService.addAdminLog(request, "禁用/启用管理员", "id=" + adminId);
            return new ResMsg(ResCode.OPERATE_SUCCESS.code(), ResCode.OPERATE_SUCCESS.msg());
        }
        return new ResMsg(ResCode.OPERATE_FAIL.code(), ResCode.OPERATE_FAIL.msg());
    }

    /**
     * 管理员编辑
     *
     * @param admin
     * @param br
     * @return
     */
    @RequestMapping("/adminEdit")
    @AuthPassport
    @ResponseBody
    public ResMsg adminEdit(@Valid Admin admin, BindingResult br, HttpServletRequest request) {
        if (br.hasErrors()) {
            return new ResMsg(1, br.getFieldError().getDefaultMessage());
        }
        if (admin.getId() == null) {
            if (StringUtil.isBlank(admin.getAdminPassword())) {
                return new ResMsg(5, "新增用户密码不能为空");
            }
            if (adminService.findAdminCountByName(admin.getAdminName()) > 0) {
                return new ResMsg(6, "用户名已存在");
            }
            adminService.insertAdmin(admin);
            systemService.addAdminLog(request, "新增管理员", "username=" + admin.getAdminName());
            return new ResMsg(ResCode.INSERT_SUCCESS.code(), ResCode.INSERT_SUCCESS.msg());
        } else {
            adminService.updateAdmin(admin);
            systemService.addAdminLog(request, "更新管理员", "username=" + admin.getAdminName());
            return new ResMsg(ResCode.UPDATE_SUCCESS.code(), ResCode.UPDATE_SUCCESS.msg());
        }
    }

    /**
     * @return
     */
    @RequestMapping("module")
    @AuthPassport
    public String module() {
        return "system/module";
    }

    /**
     * @return
     */
    @RequestMapping("/getAllModule")
    @ResponseBody
    @AuthPassport(right = false)
    public List<Map<String, Object>> getAllModule() {
        List<Map<String, Object>> mapList = new ArrayList<Map<String, Object>>();
        List<Module> list = moduleService.findModuleAll(); //所有模块
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("id", m.getId());
            map.put("pId", m.getParentId());
            map.put("name", m.getModName());
            map.put("flag", m.getModFlag());
            map.put("path", m.getModPath());
            map.put("icon", m.getModIcon());
            map.put("sort", m.getModSort());
            mapList.add(map);
        }
        return mapList;
    }

    /**
     * 模块编辑
     *
     * @return
     */
    @RequestMapping("/moduleEdit")
    @ResponseBody
    @AuthPassport
    public ResMsg moduleEdit(Integer moduleId, Integer parentId, Integer flag, String name, String path, String icon, Integer sort,
                             HttpServletRequest request) {
        if (StringUtil.isBlank(name)) {
            return new ResMsg(1, "模块名不能为空");
        }
        if (StringUtil.isBlank(path)) {
            return new ResMsg(2, "Action不能为空");
        }
        if (moduleId == null) {
            if (parentId == null) {
                return new ResMsg(3, "请选择父模块");
            }
            if (moduleService.insertModule(flag, name, parentId, path, icon, sort) > 0) {
                systemService.addAdminLog(request, "新增模块", "name=" + name);
                return new ResMsg(ResCode.INSERT_SUCCESS.code(), ResCode.INSERT_SUCCESS.msg());
            }
            return new ResMsg(ResCode.INSERT_FAIL.code(), ResCode.INSERT_FAIL.msg());
        } else {
            if (moduleService.updateModule(moduleId, name, path, icon, sort) > 0) {
                systemService.addAdminLog(request, "编辑模块", "name=" + name);
                return new ResMsg(ResCode.UPDATE_SUCCESS.code(), ResCode.UPDATE_SUCCESS.msg());
            }
            return new ResMsg(ResCode.UPDATE_FAIL.code(), ResCode.UPDATE_FAIL.msg());
        }
    }

    /**
     * 删除模块
     *
     * @param moduleId
     * @return
     */
    @RequestMapping("/moduleDel")
    @ResponseBody
    @AuthPassport
    public ResMsg moduleDel(Integer moduleId) {
        if (moduleId == null) {
            return new ResMsg(ResCode.CODE_22.code(), ResCode.CODE_22.msg());
        }
        if (moduleService.deleteModule(moduleId) > 0) {
            return new ResMsg(ResCode.DELETE_SUCCESS.code(), ResCode.DELETE_SUCCESS.msg());
        }
        return new ResMsg(ResCode.DELETE_FAIL.code(), ResCode.DELETE_FAIL.msg());
    }

    /**
     * 权限管理
     *
     * @param map
     * @return
     */
    @RequestMapping("/rights")
    @AuthPassport
    public String rights(ModelMap map) {
        List<Role> list = moduleService.findRoleList();
        map.addAttribute("roles", list);
        return "system/rights";
    }

    /**
     * 组禁用或启用
     *
     * @param roleId
     * @return
     */
    @RequestMapping("/roleEnable")
    @AuthPassport
    @ResponseBody
    public ResMsg roleEnable(Integer roleId, HttpServletRequest request) {
        if (roleId == null) {
            return new ResMsg(ResCode.CODE_22.code(), ResCode.CODE_22.msg());
        }
        if (moduleService.deleteRole(roleId) > 0) {
            systemService.addAdminLog(request, "组禁用或启用", "roleId=" + roleId);
            return new ResMsg(ResCode.OPERATE_SUCCESS.code(), ResCode.OPERATE_SUCCESS.msg());
        }
        return new ResMsg(ResCode.OPERATE_FAIL.code(), ResCode.OPERATE_FAIL.msg());
    }

    /**
     * 组编辑
     *
     * @param roleName
     * @param roleId
     * @return
     */
    @RequestMapping("/roleEdit")
    @AuthPassport
    @ResponseBody
    public ResMsg roleEdit(String roleName, Integer roleId, HttpServletRequest request) {
        if (StringUtil.isBlank(roleName)) {
            return new ResMsg(1, "组名不能为空");
        }
        if (roleId == null) {
            moduleService.insertRole(roleName);
            systemService.addAdminLog(request, "新增组", "roleName=" + roleName);
            return new ResMsg(ResCode.INSERT_SUCCESS.code(), ResCode.INSERT_SUCCESS.msg());
        }
        moduleService.updateRole(roleId, roleName);
        systemService.addAdminLog(request, "编辑组", "roleName=" + roleName);
        return new ResMsg(ResCode.UPDATE_SUCCESS.code(), ResCode.UPDATE_SUCCESS.msg());
    }

    /**
     * 获取权限(组或用户)
     *
     * @param roleId
     * @param adminId
     * @return
     */
    @RequestMapping("/permits")
    @AuthPassport
    @ResponseBody
    public List<Tree> permits(Integer roleId, Integer adminId) {
        // ztree data
        List<Tree> treeList = new ArrayList<Tree>();
        // 所有模块
        List<Module> list = moduleService.findModuleAll();
        List<Module> mods = moduleService.findModuleByRoleOrAdmin(roleId, adminId);

        for (int i = 0, j = list.size(); i < j; i++) {
            Module m = list.get(i);
            // 组织tree
            Tree tree = new Tree();
            tree.setId(m.getId());
            tree.setpId(m.getParentId());
            tree.setName(m.getModName());
            if (mods == null || mods.isEmpty()) {
                treeList.add(tree);
                continue;
            }
            for (Module roleMenu : mods) { // 匹配
                // System.out.println(roleMenu.getId()+"=="+m.getId()+"?"+(roleMenu.getId().equals(m.getId())));
                if (roleMenu.getId().equals(m.getId())) {
                    tree.setChecked(true); // 选中状态
                    break;
                }
            }
            treeList.add(tree);
        }
        return treeList;
    }

    /**
     * 授权(组或用户)
     *
     * @param roleId
     * @param adminId
     * @param params
     * @param request
     * @return
     */
    @RequestMapping("/permit")
    @AuthPassport
    @ResponseBody
    public ResMsg permit(Integer roleId, Integer adminId, String params, HttpServletRequest request) {
        if (roleId == null && adminId == null) {
            return new ResMsg(ResCode.CODE_22.code(), ResCode.CODE_22.msg());
        }

        // 模块id集合
        String[] rights = params.split(",");
        Integer[] mid = new Integer[rights.length];
        for (int i = 0, b = rights.length; i < b; i++) {
            mid[i] = Integer.parseInt(rights[i]);
        }

        if (roleId != null) {
            if (StringUtil.isBlank(params)) {
                moduleService.deleteRights(roleId);
                return new ResMsg(0, "已取消组所有权限");
            }
            moduleService.permitRole(roleId, rights);
            systemService.addAdminLog(request, "授权组", "roleId=" + roleId);
        }

        if (adminId != null) {
            if (StringUtil.isBlank(params)) {
                adminService.deleteRights(adminId);
                return new ResMsg(0, "已取消用户所有权限");
            }
            moduleService.permitAdmin(adminId, rights);
            systemService.addAdminLog(request, "授权用户", "adminId=" + adminId);
        }

        return new ResMsg(0, "授权成功");
    }

    @RequestMapping("/address")
    @AuthPassport
    public String address() {
        return "system/address";
    }

    /**
     * 获取地址
     *
     * @param id
     * @return
     */
    @RequestMapping("/getAddr")
    @AuthPassport(right = false)
    @ResponseBody
    public List<Map<String, Object>> getAddr(Integer id) {
        Address condition = new Address();
        if (id == null) {
            condition.setLevel(1);
        }
        condition.setParent(id);
        List<Map<String, Object>> mapList = new ArrayList<Map<String, Object>>();
        List<Address> list = addrService.findAddrList(condition);
        for (int i = 0; i < list.size(); i++) {
            Address address = list.get(i);
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("id", address.getId());
            map.put("pId", address.getParent());
            map.put("name", address.getName());
            map.put("level", address.getLevel());
            if (address.getLevel() != 4) {
                map.put("isParent", true);
            }
            mapList.add(map);
        }
        return mapList;
    }

    /**
     * 地址编辑
     *
     * @param id
     * @param name
     * @param parent
     * @param level
     * @return
     */
    @RequestMapping("/addrEdit")
    @ResponseBody
    @AuthPassport
    public ResMsg addrEdit(Integer id, String name, Integer parent, Integer level, HttpServletRequest request) {
        if (id == null) { //新增
            Address address = new Address();
            address.setName(name);
            address.setParent(parent);
            address.setLevel(level + 1);
            if (addrService.insert(address) > 0) {
                return new ResMsg(ResCode.INSERT_SUCCESS.code(), ResCode.INSERT_SUCCESS.msg());
            }
            return new ResMsg(ResCode.INSERT_FAIL.code(), ResCode.INSERT_FAIL.msg());
        }
        //更新
        Address address = new Address(id);
        address.setParent(parent);
        address.setName(name);
        if (addrService.update(address) > 0) {
            systemService.addAdminLog(request, "编辑地址", "name=" + name);
            return new ResMsg(ResCode.UPDATE_SUCCESS.code(), ResCode.UPDATE_SUCCESS.msg());
        }
        return new ResMsg(ResCode.UPDATE_FAIL.code(), ResCode.UPDATE_FAIL.msg());
    }

    /**
     * @param id
     * @return
     */
    @RequestMapping("/addrDel")
    @ResponseBody
    @AuthPassport
    public ResMsg addrDel(Integer id) {
        if (id == null) {
            return new ResMsg(ResCode.CODE_22.code(), ResCode.CODE_22.msg());
        }
        if (addrService.delete(id) > 0) {
            return new ResMsg(ResCode.DELETE_SUCCESS.code(), ResCode.DELETE_SUCCESS.msg());
        }
        return new ResMsg(ResCode.DELETE_FAIL.code(), ResCode.DELETE_FAIL.msg());
    }

    /**
     * 下载地址JS文件
     *
     * @param response
     */
    @RequestMapping("/addrGenc")
    @AuthPassport(right = false)
    public void addrGenc(HttpServletResponse response) {
        response.setContentType("multipart/form-data");
        response.setHeader("Content-Disposition", "attachment;fileName=city-picker.data.all.js");
        String addr = addrService.genAddr();
        try {
            OutputStream os = response.getOutputStream();
            os.write(addr.getBytes(Charset.forName("UTF-8")));
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 执行查询操作
     *
     * @param session
     * @param sql
     * @return
     */
    @SuppressWarnings("unchecked")
    @RequestMapping("/executeQuery")
    @AuthPassport
    @ResponseBody
    public Map<String, Object> executeQuery(String sql, HttpSession session, HttpServletRequest request) throws
            Exception {
        ResMsg msg = null;
        Map<String, Object> map = new HashMap<String, Object>();
        if (StringUtil.isBlank(sql)) {
            msg = new ResMsg(1, "sql不能为空");
            map.put("msg", msg);
            return map;
        }
        if (!sql.startsWith("select") && !sql.startsWith("SELECT")) {
            msg = new ResMsg(2, "sql非查询语句");
            map.put("msg", msg);
            return map;
        }
        // 列名集合
        List<String> columnList = new ArrayList<String>();
        // 数据集合
        List<List<Object>> dataList = new ArrayList<List<Object>>();
        long startTime = System.currentTimeMillis();
        Object[] arrOb = systemService.executeQuery(sql);
        long endTime = System.currentTimeMillis();
        // 存入数据库查询时间
        map.put("rTime", String.valueOf((endTime - startTime) / 1000.000));
        if (null != arrOb) {
            columnList = (List<String>) arrOb[0];
            dataList = (List<List<Object>>) arrOb[1];
            msg = new ResMsg(0, "success");
        } else {
            msg = new ResMsg(3, "fail");
        }

        // 存放字段名
        map.put("columnList", columnList);
        // 存放数据(从数据库读出来的数据)
        map.put("dataList", dataList);
        map.put("msg", msg);
        systemService.addAdminLog(request, "执行查询SQL", "sql=" + sql);
        return map;
    }

    /**
     * 执行更新操作
     *
     * @param session
     * @param sql
     * @return
     */
    @RequestMapping("/executeUpdate")
    @AuthPassport
    @ResponseBody
    public Object executeUpdate(String sql, HttpSession session, HttpServletRequest request) throws Exception {
        ResMsg msg = null;
        Map<String, Object> map = new HashMap<String, Object>();
        if (StringUtil.isBlank(sql)) {
            msg = new ResMsg(1, "sql不能为空");
            map.put("msg", msg);
            return map;
        }
        if (!sql.contains("update") && !sql.contains("delete") && !sql.contains("insert")) {
            msg = new ResMsg(2, "sql非更新语句");
            map.put("msg", msg);
            return map;
        }
        long startTime = System.currentTimeMillis();
        systemService.executeUpdate(sql);
        msg = new ResMsg(0, "success");
        long endTime = System.currentTimeMillis();

        map.put("rTime", String.valueOf((endTime - startTime) / 1000.000));
        map.put("msg", msg);
        systemService.addAdminLog(request, "执行更新SQL", "sql=" + sql);
        return map;
    }

    /**
     * 获取日志目录（近30天）
     *
     * @return
     */
    @Deprecated
    @RequestMapping("/getLogList")
    @AuthPassport
    @ResponseBody
    public Object getLogList() {
        List<Map<String, String>> list = FileUtil.getLogFileList(config.getLogPath());
        return list;
    }

    /**
     * 系统日志列表
     *
     * @param condition
     * @param map
     * @return
     */
    @RequestMapping("/syslogList")
    @AuthPassport
    public String syslogList(ModelMap map) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        String level = loggerContext.getLogger("com.jf.database.mapper").getLevel().toString();
        map.addAttribute("level", level);
        return "system/syslog";
    }

    /**
     * 获取系统日志目录
     *
     * @param path
     * @return
     */
    @RequestMapping("/getLogDirectory")
    @AuthPassport
    @ResponseBody
    public ResMsg getLogDirectory(String path) {
        if (StringUtil.isBlank(path)) {
            return new ResMsg(1, "未指定路径", null);
        }
        if (!path.startsWith("/")) {
            return new ResMsg(2, "路径必须以斜杠/开头", null);
        }
        List<Directory> list = FileUtil.getDirectory(path, config.getLogPath());
        if (list == null) {
            return new ResMsg(3, "路径不存在", null);
        }
        return new ResMsg(ResCode.SUCCESS.code(), ResCode.SUCCESS.msg(), list);
    }

    /**
     * 打印SQL开关
     *
     * @param level
     * @param request
     * @return
     */
    @RequestMapping("/sqlLevel")
    @AuthPassport
    @ResponseBody
    public ResMsg sqlLevel(String level, HttpServletRequest request) {
        if (StringUtil.isBlank(level)) {
            return new ResMsg(1, "level is null");
        }
        if (!"INFO".equals(level) && !"DEBUG".equals(level)) {
            return new ResMsg(1, "level must be INFO or DEBUG");
        }
        Admin admin = getSession(request, IConstant.SESSION_ADMIN);
        if (admin == null || admin.getRole().getRoleFlag() != 0) {
            return new ResMsg(1, "refuse if superadmin");
        }

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getLogger("com.jf.database.mapper").setLevel(Level.valueOf(level));
        loggerContext.getLogger("com.jf.mapper").setLevel(Level.valueOf(level));

        return new ResMsg(ResCode.SUCCESS.code(), ResCode.SUCCESS.msg());
    }

    /**
     * 读取系统日志
     * <p>流式输出</p>
     *
     * @param response
     * @param request
     * @return
     */
    @RequestMapping("/downLog")
    @AuthPassport
    public void downLog(HttpServletResponse response, HttpServletRequest request) {
        String fileName = request.getParameter("fileName");
        if (fileName == null || "".equals(fileName)) {
            return;
        }
        try {
            InputStream inputStream = new FileInputStream(new File(config.getLogPath() + fileName));
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            OutputStream os = response.getOutputStream();

            os.write("<div id='main' style='font-family: Consolas;font-size: 14px;'>".getBytes("UTF-8"));
            String str = null;
            while ((str = bufferedReader.readLine()) != null) {
                os.write(("<p>" + str + "</p>").getBytes());
            }
            os.write("</div><script src=\"/static/library/plugins/ansi/ansi_up.js\" type=\"text/javascript\"></script><script type=\"text/javascript\">var txt  = document.getElementById('main').innerHTML;var ansi_up = new AnsiUp;ansi_up.escape_for_html = false;var html = ansi_up.ansi_to_html(txt);var cdiv = document.getElementById('main');cdiv.innerHTML = html;</script>".getBytes("UTF-8"));

            os.close();
            inputStream.close();
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    /**
     * 操作日志列表
     *
     * @param condition
     * @param map
     * @return
     */
    @RequestMapping("/logList")
    @AuthPassport
    public String logList(Log condition, ModelMap map) {
        PageInfo pageInfo = systemService.findLogByPage(condition);
        map.addAttribute("pageInfo", pageInfo);
        map.addAllAttributes(BeanUtil.beanToMap(condition));
        map.put("logCount", systemService.findLogCount());
        return "system/log";
    }

    /**
     * 操作日志备份
     *
     * @param monthAgo
     * @return
     */
    @RequestMapping("/backupLog")
    @AuthPassport
    public ModelAndView backupLog(String monthAgo) {
        Integer ago = Convert.stringToInt(monthAgo, 1);
        Map<String, Object> model = null;
        try {
            model = new HashMap<String, Object>();
            model.put("list", systemService.findOldLog(ago));
            model.put("header", new String[]{"username", "remark", "params", "createDate"});
            model.put("headerVal", new String[]{"用户名", "备注", "参数", "时间"});
            model.put("ename", "操作日志备份" + DateUtil.getCurrentTime(2));
        } catch (Exception e) {
            return null;
        }
        systemService.deleteOldLog(ago);
        return new ModelAndView(new ViewExcel(), model);
    }

    /**
     * 获取模板内容
     *
     * @param name
     * @param type
     * @return
     */
    @RequestMapping("/getTemplet")
    @AuthPassport
    @ResponseBody
    @Deprecated
    public ResMsg getTemplet(String name, String type) {
        if (StringUtil.isBlank(name)) {
            return new ResMsg(1, "invalid param name");
        }
        Integer TYPE = Convert.stringToInt(type, -1);
        String path = "";
        if (TYPE == 1) { // email
            path = "email";
        } else if (TYPE == 2) { // sms
            path = "sms";
        } else {
            return new ResMsg(2, "invalid param type");
        }
        String file = config.getStaticPath() + "WEB-INF/tpl/" + path + "/" + name + ".ftl";
        String output = "";
        File src = new File(file);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(src));
            String line = null;
            while ((line = reader.readLine()) != null) {
                output += line;
            }
            reader.close();
            return new ResMsg(0, output);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResMsg(3, "未找到模板");
        }
    }

    /**
     * 更新模板内容
     *
     * @param name
     * @param txt
     * @param type
     * @return
     */
    @RequestMapping("/updateTemplet")
    @AuthPassport
    @ResponseBody
    @Deprecated
    public ResMsg updateTemplet(String name, String txt, String type) {
        if (StringUtil.isBlank(name)) {
            return new ResMsg(1, "invalid param name");
        }
        if (StringUtil.isBlank(txt)) {
            return new ResMsg(2, "invalid param txt");
        }
        Integer TYPE = Convert.stringToInt(type, -1);
        String path = "";
        if (TYPE == 1) {
            path = "emailTpl";
        } else if (TYPE == 2) {
            path = "smsTpl";
        } else {
            return new ResMsg(3, "invalid param type");
        }
        String file = config.getStaticPath() + "WEB-INF/tpl/" + path + "/" + name + ".ftl";
        File src = new File(file);
        OutputStream os = null;
        try {
            os = new FileOutputStream(src, false);
            if (TYPE == 1) { // html邮件
                txt = "<html><head><meta http-equiv=\"content-type\" content=\"text/html;" +
                        "charset=utf8\"></head><body>" + txt + "</body></html> ";
            }
            byte[] data = txt.getBytes();
            os.write(data, 0, data.length);
            os.close();
            os.flush();
            return new ResMsg(0, "保存成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new ResMsg(4, "未找到模板");
        }
    }

    // ***************邮件短信测试 ***************//
    /*Map<String, Object> map=new HashMap<String, Object>();
    map.put("username", "xujunfei");
	emailService.send(map,"809573150@qq.com", "Test", "register");
	  
	String code = StringUtil.getSmsCode(6); 
	Map<String, String> root=new HashMap<String, String>(); 
	root.put("code", code); sms.sendSms(root, "17730215423", "code");*/

    /**
     * 获取服务器静态文件目录(/static/)
     *
     * @return
     */
    @RequestMapping("/getDirectory")
    @AuthPassport
    @ResponseBody
    public ResMsg getDirectory(String path) {
        if (StringUtil.isBlank(path)) {
            return new ResMsg(1, "未指定路径");
        }
        if (!path.startsWith("/")) {
            return new ResMsg(2, "路径必须以斜杠/开头");
        }
        List<Directory> list = FileUtil.getDirectory(path, config.getStaticPath());
        if (list == null) {
            return new ResMsg(3, "路径不存在");
        }
        return new ResMsg(ResCode.SUCCESS.code(), ResCode.SUCCESS.msg(), list);
    }

}
