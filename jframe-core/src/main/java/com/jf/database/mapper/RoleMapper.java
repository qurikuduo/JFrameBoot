package com.jf.database.mapper;

import com.jf.database.model.Role;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * RoleMapper
 * @author rick
 * @version
 */
public interface RoleMapper {

	List<Role> findAll();

	int insert(Role bean);

	int update(Role bean);

    int delete(Long id);

	/**
	 * 检查权限
	 * @return 1 | 0
	 */
	@Deprecated
	int checkRights(@Param("roleId") Long roleId, @Param("modPath") String modPath);

}
