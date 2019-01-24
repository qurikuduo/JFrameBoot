<!DOCTYPE html><html><head>    <meta charset="utf-8">    <meta http-equiv="X-UA-Compatible" content="IE=edge">    <meta name="viewport" content="width=device-width, initial-scale=1.0">    <meta name="renderer" content="webkit">    <title></title>    <link href="/static/common/css/bootstrap.min.css" rel="stylesheet">    <link href="/static/theme/css/style.min.css" rel="stylesheet"></head><body style="padding: 15px;"><div class="row">    <div class="col-sm-3">        <div class="ibox float-e-margins">            <div class="ibox-title">                <span class="label label-success pull-right">月</span>                <h5>收入</h5>            </div>            <div class="ibox-content">                <h1 class="no-margins">2000.00</h1>                <div class="stat-percent font-bold text-success">98% <i class="fa fa-bolt"></i>                </div>                <small>总收入</small>            </div>        </div>    </div>    <div class="col-sm-3">        <div class="ibox float-e-margins">            <div class="ibox-title">                <span class="label label-info pull-right">全年</span>                <h5>订单</h5>            </div>            <div class="ibox-content">                <h1 class="no-margins">13822.99</h1>                <div class="stat-percent font-bold text-info">20% <i class="fa fa-level-up"></i>                </div>                <small>新订单</small>            </div>        </div>    </div>    <div class="col-sm-3">        <div class="ibox float-e-margins">            <div class="ibox-title">                <span class="label label-primary pull-right">今天</span>                <h5>访客</h5>            </div>            <div class="ibox-content">                <h1 class="no-margins">322.90</h1>                <div class="stat-percent font-bold text-navy">44% <i class="fa fa-level-up"></i>                </div>                <small>新访客</small>            </div>        </div>    </div>    <div class="col-sm-3">        <div class="ibox float-e-margins">            <div class="ibox-title">                <span class="label label-danger pull-right">最近一个月</span>                <h5>活跃用户</h5>            </div>            <div class="ibox-content">                <h1 class="no-margins">14000</h1>                <div class="stat-percent font-bold text-danger">38% <i class="fa fa-level-down"></i>                </div>                <small></small>            </div>        </div>    </div></div><div class="row">    <div class="col-md-6">        <div class="ibox-content" id="charts1" style="height: 420px;"></div>    </div>    <div class="col-md-6">        <div class="ibox-content" id="charts2" style="height: 420px;"></div>    </div></div><br><br><div class="mt10">    <span class="label label-info">JFRAME ${version}</span>    <span class="label label-info">${info}</span>    <span class="label label-info">        <i class="fa fa-flash"></i>${arr[4]}GB／${arr[5]}GB        <i class="fa fa-server"></i>${arr[1]}GB／${arr[2]}GB    </span>    <span class="label label-default pull-right" id="date">NOW: 00:00:00</span></div><!-- 菜单类型-适合B2B2C --><#--<div class="row">    <div class="col-md-4 home-panel">        <div class="home-item">            <div class="home-item-head">                <div class="ico"><i class="fa fa-user"></i></div>                <h3>菜单</h3>                <h5>菜单1／菜单2</h5>            </div>            <div class="home-item-footer">                <ul>                    <li class="w50"><a href="/">菜单1</a></li>                    <li class="w50"><a href="/">菜单2</a></li>                </ul>            </div>        </div>    </div>    <div class="col-md-4 home-panel">        <div class="home-item">            <div class="home-item-head">                <div class="ico"><i class="fa fa-gear"></i></div>                <h4>${sysname} ${version}</h4>                <h4>${info} ／ <span id="date"></span></h4>                <h4>                    <i class="fa fa-flash"></i>${arr[4]}GB／${arr[5]}GB                    <i class="fa fa-server"></i>${arr[1]}GB／${arr[2]}GB                </h4>            </div>        </div>    </div></div>--><script src="/static/library/jquery/jquery-2.1.1.min.js"></script><script src="/static/library/plugins/echarts/echarts.min.js"></script><script type="text/javascript">    var myChart1 = echarts.init(document.getElementById("charts1"));    var myChart2 = echarts.init(document.getElementById("charts2"));    $(function () {        window.setInterval("serverDate()", 1000);        myChart1.showLoading({text: '正在努力的读取数据中...'});        myChart2.showLoading({text: '正在努力的读取数据中...'});        var data1x = ["安徽省", "北京市", "上海市", "广东省", "河北省", "河南省", "浙江省"]        var data1y = [            {value: 199, name: '安徽省'},            {value: 200, name: '北京市'},            {value: 389, name: '上海市'},            {value: 140, name: '广东省'},            {value: 239, name: '河北省'},            {value: 120, name: '河南省'},            {value: 95, name: '浙江省'}        ]        chart1(data1x, data1y);        var data2x = ["09-10", "09-11", "09-12", "09-13", "09-14", "09-15"]        var data2y1 = [199.9, 205.5, 344.33, 300.99, 342.99, 784.99];        var data2y2 = [12.4, 20.00, 43.9, 44.9, 90.8, 88.11];        chart2(data2x, data2y1, data2y2);    });    var sd = ${date};    function serverDate() {        var date = new Date(sd);        var timeStr = date.getFullYear() + "-" + format(date.getMonth() + 1) + "-" + format(date.getDate()) + " " +                format(date.getHours()) + ":" + format(date.getMinutes()) + ":" + format(date.getSeconds());        $("#date").html(timeStr);        sd += 1000;    }    function format(s) {        return s < 10 ? ("0" + s) : s;    }    function chart1(datax, datay) {        option = {            title: {                text: '会员分布统计',                subtext: 'Member Area',                x: 'center'            },            tooltip: {                trigger: 'item',                formatter: "{a} <br/>{b} : {c} ({d}%)"            },            legend: {                orient: 'vertical',                left: 'left',                data: datax            },            series: [                {                    name: '人数',                    type: 'pie',                    radius: '55%',                    center: ['50%', '60%'],                    data: datay,                    roseType: 'area',                    itemStyle: {                        emphasis: {                            shadowBlur: 10,                            shadowOffsetX: 0,                            shadowColor: 'rgba(0, 0, 0, 0.5)'                        }                    }                }            ]        }        myChart1.setOption(option);        myChart1.hideLoading();    }    function chart2(twx, twy, twy2) {        option = {            title: {                text: "充值提现",                subtext: "Charge&Withdraw",                x: "4%"            },            tooltip: {                trigger: "axis",                axisPointer: {                    type: 'cross',                    animation: false,                    label: {                        backgroundColor: '#505765'                    }                },                formatter: "{b} <br/> {a}:{c}"            },            grid: {                borderWidth: 0,                top: 110,                bottom: 30            },            legend: {                data: ['充值', '提现']            },            xAxis: [{                type: "category",                boundaryGap: true,                data: twx            }],            yAxis: [                {                    name: "充值",                    type: "value"                },                {                    name: "提现",                    type: "value"                }            ],            series: [                {                    name: "充值",                    type: "line",                    barMaxWidth: 35,                    barGap: "10%",                    itemStyle: {                        normal: {                            label: {                                show: true,                                position: "top",                                formatter: function (p) {                                    return p.value > 0 ? (p.value) : '';                                }                            }                        }                    },                    data: twy                },                {                    name: "提现",                    type: "line",                    yAxisIndex: 1,                    barMaxWidth: 35,                    barGap: "10%",                    itemStyle: {                        normal: {                            label: {                                show: true,                                position: "top",                                formatter: function (p) {                                    return p.value > 0 ? (p.value) : '';                                }                            }                        }                    },                    data: twy2                }            ]        }        myChart2.setOption(option);        myChart2.hideLoading();    }</script></body></html>