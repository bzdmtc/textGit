import type { Directive } from 'vue'
import { useAuthStore } from '@/stores/auth'

interface PrivBinding {
  module: string
  method: string
}

const privilege: Directive<HTMLElement, PrivBinding> = {
  mounted(el, binding) {
    const store = useAuthStore()
    if (!binding.value || !store.hasPriv(binding.value.module, binding.value.method)) {
      el.parentNode?.removeChild(el)
    }
  },
}

export default privilege
