<script>
import { h } from 'vue'
import SvgIcon from '@/components/SvgIcon/index.vue'

function isValidIcon(icon) {
  return icon && icon !== '#'
}

export default {
  name: 'MenuItem',
  props: {
    icon: {
      type: String,
      default: ''
    },
    title: {
      type: String,
      default: ''
    }
  },
  setup(props) {
    return () => {
      const vnodes = []
      if (isValidIcon(props.icon)) {
        vnodes.push(h(SvgIcon, { iconClass: props.icon }))
      }
      if (props.title) {
        const spanProps = props.title.length > 5 ? { title: props.title } : undefined
        vnodes.push(h('span', spanProps, props.title))
      }
      return vnodes
    }
  }
}
</script>
