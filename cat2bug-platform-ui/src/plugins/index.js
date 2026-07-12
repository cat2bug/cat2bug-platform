import tab from './tab'
import auth from './auth'
import cache from './cache'
import modal from './modal'
import download from './download'
import topic from './topic'

export default {
  install(app) {
    app.config.globalProperties.$tab = tab
    app.config.globalProperties.$auth = auth
    app.config.globalProperties.$cache = cache
    app.config.globalProperties.$modal = modal
    app.config.globalProperties.$download = download
    app.config.globalProperties.$topic = topic
  }
}
