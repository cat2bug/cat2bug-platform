import { getCurrentInstance } from 'vue'

/**
 * Composable wrapper around the WebSocket plugin ($connect / $socket / $disconnect).
 */
export function useWebSocket() {
  const instance = getCurrentInstance()
  const gp = instance?.appContext?.config?.globalProperties

  return {
    get socket() {
      return gp?.$socket ?? null
    },
    connect(url, options) {
      gp?.$connect?.(url, options)
    },
    disconnect() {
      gp?.$disconnect?.()
    }
  }
}
