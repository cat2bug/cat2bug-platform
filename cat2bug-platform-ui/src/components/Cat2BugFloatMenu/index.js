import Cat2BugFloatMenu from "@/components/Cat2BugFloatMenu/Cat2BugFloatMenu";

// 全局浮动菜单
export default {
  install(Vue) {
    // 创建组件构造函数
    const menu = Vue.extend(Cat2BugFloatMenu)
    // 注册全局组件
    Vue.component('floatMenu', menu)
    // 实例化组件对象
    const instance = new menu({
      el: document.createElement('div')
    });
    // 挂载
    let vm = instance.$mount();
    // 添加到body里边
    document.body.appendChild(vm.$el)

    // 创建函数(传入配置对象)
    const publicMethods = {
      /** 初始化需要操作的窗口配置 */
      windowsInit(el) {
        // 先释放旧的
        vm.windowsDestory();
        // 再初始化新的
        vm.windowsInit(el);
      },
      /** 释放需要操作的窗口配置 */
      windowsDestory() {
        vm.windowsDestory();
      },
      /** 添加惨淡 */
      addMenus(menus) {
        vm.addMenus(menus);
      },
      /** 移除惨淡 */
      removeMenu(id) {
        vm.removeMenu(id);
      },
      /** 添加惨淡 */
      resetMenus(menus) {
        vm.resetMenus();
        if(menus && menus.length>0) {
          vm.addMenus(menus)
        }
      }
    }
    Vue.prototype.$floatMenu = publicMethods; // 挂到Vue的原型上使用
  }
}
