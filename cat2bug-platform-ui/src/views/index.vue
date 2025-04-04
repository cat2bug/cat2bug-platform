<template>
  <div class="app-container home">
    <el-row :gutter="20">
      <el-col :sm="24" :lg="12" style="padding-left: 20px">
        <h2>{{$t('system-name')}}</h2>
        <p>{{$t('system-introduce')}}</p>
        <p>
          <b>{{ $t('version') }}:</b> <span>v{{ version }}</span>
        </p>
        <p>
          <el-tag type="danger">{{$t('free-open-source')}}</el-tag>
        </p>
        <p>
          <el-button
            type="primary"
            size="mini"
            icon="el-icon-cloudy"
            plain
            @click="goTarget('https://github.com/cat2bug/cat2bug-platform')"
          >{{$t('visit-github')}}</el-button
          >
          <el-button
            type="primary"
            size="mini"
            icon="el-icon-cloudy"
            plain
            @click="goTarget('https://gitee.com/cat2bug/cat2bug-platform')"
            >{{$t('visit-gitee')}}</el-button
          >
          <el-button
            size="mini"
            icon="el-icon-s-home"
            plain
            @click="goTarget('https://www.cat2bug.com')"
            >{{$t('visit-homepage')}}</el-button
          >
        </p>
      </el-col>

      <el-col :sm="24" :lg="12" style="padding-left: 50px">
        <el-row>
          <el-col :span="12">
            <h2>{{$t('technical-selection')}}</h2>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="6">
            <h4>{{$t('back-end-technology')}}</h4>
            <ul>
              <li>SpringBoot</li>
              <li>Spring Security</li>
              <li>JWT</li>
              <li>MyBatis</li>
              <li>Druid</li>
              <li>Fastjson</li>
              <li>...</li>
            </ul>
          </el-col>
          <el-col :span="6">
            <h4>{{$t('front-end-technology')}}</h4>
            <ul>
              <li>Vue</li>
              <li>Vuex</li>
              <li>Element-ui</li>
              <li>Axios</li>
              <li>Sass</li>
              <li>Quill</li>
              <li>...</li>
            </ul>
          </el-col>
        </el-row>
      </el-col>
    </el-row>
    <el-divider />
    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="12" :lg="8">
        <el-card class="update-log">
          <div slot="header" class="clearfix">
            <span>{{$t('contact-information')}}</span>
          </div>
          <div class="body">
            <p>
              <i class="el-icon-s-home"></i> {{$t('website')}}：<el-link
                href="https://www.cat2bug.com"
                target="_blank"
                >https://www.cat2bug.com</el-link
              >
            </p>
            <p>
              <i class="el-icon-user-solid"></i> {{$t('qq-group')}}：
<!--              <s> 满12345 </s>-->
              <a target="_blank" href="https://qm.qq.com/cgi-bin/qm/qr?k=vpZaR_tWTwG0lPABP9NqgX2wSSSEEaWV&jump_from=webapi&authKey=8xIzBRe5T9RoEdisRBgYWbMmCoWXIYk4VikARN8S4RA3MZT7z7FUj71ZIIvEzGzq">731462000</a>
            </p>
            <el-image
              class="qcoder"
              :src="require('@/assets/images/qq-qcoder.png')"
              fit="cover"></el-image>
            <el-image
              class="qcoder"
              :src="require('@/assets/images/wechat-qcoder.png')"
              fit="cover"></el-image>
          </div>
        </el-card>

        <el-card class="update-log margin-top-20">
          <div slot="header" class="clearfix">
            <span>{{$t('ecosystem')}}</span>
          </div>
          <el-collapse accordion>
            <el-collapse-item title="Cat2Bug-JUnit">
              <div class="body">
                <p>
                  <i class="el-icon-s-order"></i> {{$t('project.introduction')}}：
                  自动化接口单元测试框架
                </p>
                <p>
                  <i class="el-icon-s-home"></i>{{$t('source-code-address')}}：<el-link
                  href="https://gitee.com/cat2bug/cat2bug-junit"
                  target="_blank">https://gitee.com/cat2bug/cat2bug-junit</el-link>
                </p>
              </div>
            </el-collapse-item>
            <el-collapse-item title="Cat2Bug-JLog">
              <div class="body">
                <p>
                  <i class="el-icon-s-order"></i> {{$t('project.introduction')}}：
                  异常日志采集框架
                </p>
                <p>
                  <i class="el-icon-s-home"></i>{{$t('source-code-address')}}：<el-link
                  href="https://gitee.com/cat2bug/cat2bug-jlog"
                  target="_blank">https://gitee.com/cat2bug/cat2bug-jlog</el-link>
                </p>
              </div>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12" :lg="8">
        <el-card class="update-log">
          <div slot="header" class="clearfix">
            <span>{{$t('update-log')}}</span>
          </div>
          <el-collapse accordion>
            <el-collapse-item title="v0.5.1 - 2025-03-15">
              <ol>
                <li>添加仪表盘</li>
                <li>在测试用例中添加按级别查找功能</li>
                <li>在测试计划中添加取消通过功能</li>
                <li>在admin账户中添加权限管理</li>
                <li>修复系统功能BUG</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v0.5.0 - 2025-01-18">
              <ol>
                <li>新增测试计划</li>
                <li>新增同步项目数据到云平台</li>
                <li>修复系统功能BUG</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v0.4.2 - 2024-08-15">
              <ol>
                <li>在测试用例、缺陷、交付物、报告、文档、通知模块新增悬浮菜单</li>
                <li>新建缺陷时添加缓存上一次选项功能</li>
                <li>缺陷中添加excel导入导出功能</li>
                <li>添加数据库自动升级功能</li>
                <li>修复系统功能BUG</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v0.4.1 - 2024-07-21">
              <ol>
                <li>新增通知服务</li>
                <li>缺陷中添加计划开始时间和结束时间属性</li>
                <li>缺陷列表中优化图片显示方式</li>
                <li>缺陷列表中添加日历显示组件</li>
                <li>将缺陷描述设置为非必填项</li>
                <li>测试用例添加添加图片功能</li>
                <li>在每个页面中新增项目切换功能</li>
                <li>修复系统功能BUG</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v0.4.0 - 2024-06-26">
              <ol>
                <li>修复系统功能BUG</li>
                <li>添加AI私有化服务</li>
                <li>在新建缺陷时，更新描述组件，使之可以通过AI分析添加填充其它属性功能</li>
                <li>更新AI创建测试用例功能，使之可以用过AI的上下文语义理解功能，通过多次描述用例需求来调整测试用例</li>
                <li>在报告中添加模版功能</li>
                <li>新增文档功能</li>
                <li>在项目设置中新增AI配置项</li>
                <li>在缺陷中，为每个缺陷添加存活时常、驳回次数等指标</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v0.3.2 - 2024-04-19">
              <ol>
                <li>修复系统功能BUG</li>
                <li>添加分享功能</li>
                <li>添加缺陷剪贴板</li>
                <li>添加人生格言窗口</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v0.3.1 - 2024-04-05">
              <ol>
                <li>修复系统功能BUG</li>
                <li>推送报告的OPEN API接口中添加KEY属性</li>
                <li>缺陷页面添加自定义页签</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v0.3.0 - 2024-03-25">
              <ol>
                <li>添加报告功能</li>
                <li>添加系统多语言切换功能</li>
                <li>在缺陷和报告详情中，添加markdown格式展示功能，丰富数据的展现形式</li>
                <li>在测试用例、缺陷、报告页面中，实时显示成员所操作的数据项</li>
                <li>在缺陷统计中添加用户面板</li>
                <li>API接口屏蔽缺陷上传接口；添加报告上传接口</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v0.2.2 - 2024-03-04">
              <ol>
                <li>修复系统功能BUG</li>
                <li>优化缺陷中的显示样式，缺陷描述可以通过MD文件格式展示</li>
                <li>实现平台与Cat2Bug-Spring-Boot-JUnit框架对接功能</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v0.2.1 - 2024-03-01">
              <ol>
                <li>修复已有问题BUG</li>
                <li>添加在缺陷日志中可以评论的功能</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v0.2.0 - 2024-02-23">
              <ol>
                <li>修复已有问题BUG</li>
                <li>添加Mysql、H2多数据库类型部署方式</li>
                <li>添加Open API功能(测试)</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v0.1.1 - 2024-02-07">
              <ol>
                <li>增加AI创建测试用例功能</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v0.1.0 - 2024-02-02">
              <ol>
                <li>增加测试用例模块</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v0.0.2 - 2024-01-26">
              <ol>
                <li>增加系统管理模块</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="v0.0.1 - 2024-01-10">
              <ol>
                <li>Cat2Bug正式发布</li>
              </ol>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12" :lg="8">
        <el-card class="update-log">
          <div slot="header" class="clearfix">
            <span>{{$t('thanks-formally')}}</span>
          </div>
          <el-row style="margin-bottom: 20px;">
            <span>{{ $t('thanks-content') }}</span>
          </el-row>
          <cat2-bug-avatar style="margin-right: 10px;margin-bottom: 20px;" size="large" :member="member" v-for="(member,index) in tanksMemberList" :key="index">
            <template v-slot:extend>
              <span>{{member.name}}</span>
            </template>
          </cat2-bug-avatar>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import {getVersion} from "@/api/version";
import Cat2BugAvatar from "@/components/Cat2BugAvatar";

export default {
  name: "Index",
  components: {Cat2BugAvatar},
  data() {
    return {
      // 版本号
      version: null,
      tanksMemberList: [{
        name: '未完成',
        avatarUrl: require('@/assets/images/thanks/weiwancheng.png')
      },{
        name: '缥缈孤鸿影',
        avatarUrl: require('@/assets/images/thanks/piaomiaoguhongying.png')
      },{
        name: '良才ᝰ Cái',
        avatarUrl: require('@/assets/images/thanks/liangcai.png')
      },{
        name: 'wssdy',
        avatarUrl: require('@/assets/images/thanks/wssdy.png')
      },{
        name: '若即若离',
        avatarUrl: require('@/assets/images/thanks/ruojiruoli.jpg')
      },{
        name: '完美er',
        avatarUrl: require('@/assets/images/thanks/wanmeier.png')
      },{
        name: '丁为康',
        avatarUrl: require('@/assets/images/thanks/dingweikang.png')
      },{
        name: 'Richard CHAN',
        avatarUrl: require('@/assets/images/thanks/Richard-CHAN.png')
      },{
        name: 'u\'d Like',
        avatarUrl: require('@/assets/images/thanks/u-d-Like.jpg')
      },{
        name: 'Silence、',
        avatarUrl: require('@/assets/images/thanks/Silence.png')
      }]
    };
  },
  created() {
    this.getSystemVersion();
  },
  mounted() {
    this.initFloatMenu();
  },
  // 移除滚动条监听
  destroyed() {
    this.$floatMenu.windowsDestory();
  },
  methods: {
    /** 初始化浮动菜单 */
    initFloatMenu() {
      this.$floatMenu.windowsInit(document.querySelector('.main-container'));
      this.$floatMenu.resetMenus([]);
    },
    goTarget(href) {
      window.open(href, "_blank");
    },
    getSystemVersion() {
      getVersion().then(res=>{
        this.version = res;
      })
    }
  }
};
</script>

<style scoped lang="scss">
.home {
  blockquote {
    padding: 10px 20px;
    margin: 0 0 20px;
    font-size: 17.5px;
    border-left: 5px solid #eee;
  }
  hr {
    margin-top: 20px;
    margin-bottom: 20px;
    border: 0;
    border-top: 1px solid #eee;
  }
  .col-item {
    margin-bottom: 20px;
  }

  ul {
    padding: 0;
    margin: 0;
  }

  font-family: "open sans", "Helvetica Neue", Helvetica, Arial, sans-serif;
  font-size: 13px;
  color: #676a6c;
  overflow-x: hidden;

  ul {
    list-style-type: none;
  }

  h4 {
    margin-top: 0px;
  }

  h2 {
    margin-top: 10px;
    font-size: 26px;
    font-weight: 100;
  }

  p {
    margin-top: 10px;

    b {
      font-weight: 700;
    }
  }

  .qcoder {
    width: 150px;
    height: 150px;
    margin-right: 10px;
    :last-child {
      margin-right: 0px;
    }
  }
  .update-log {
    margin-bottom: 20px;
    ol {
      display: block;
      list-style-type: decimal;
      margin-block-start: 1em;
      margin-block-end: 1em;
      margin-inline-start: 0;
      margin-inline-end: 0;
      padding-inline-start: 40px;
    }
    > ::v-deep .el-card__body {
      > .member-avatar {
        margin-right: 0px !important;
        width: calc( 100% / 4 );
        justify-content: start;
      }
    }
  }
}
</style>

