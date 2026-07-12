/**
 * Minimal WebSocket plugin for Vue 3 (replaces vue-native-websocket).
 * Exposes $connect / $socket / $disconnect on globalProperties.
 */
export default {
  install(app, defaultOptions = {}) {
    let socket = null
    let options = { ...defaultOptions }

    const ws = {
      get onopen() { return socket?.onopen },
      set onopen(fn) { if (socket) socket.onopen = fn },
      get onerror() { return socket?.onerror },
      set onerror(fn) { if (socket) socket.onerror = fn },
      get onmessage() { return socket?.onmessage },
      set onmessage(fn) { if (socket) socket.onmessage = fn },
      get onclose() { return socket?.onclose },
      set onclose(fn) { if (socket) socket.onclose = fn }
    }

    app.config.globalProperties.$socket = ws
    app.config.globalProperties.$connect = (url, _connectOptions = {}) => {
      if (url) openSocket(url)
    }
    app.config.globalProperties.$disconnect = () => {
      if (socket) {
        socket.close()
        socket = null
      }
    }

    function openSocket(url) {
      if (socket) {
        socket.close()
      }
      socket = new WebSocket(url)
      app.config.globalProperties.$socket = socket
    }

    app.config.globalProperties._openWebSocket = openSocket
  }
}
