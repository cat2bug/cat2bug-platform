/**
 * Vue 3 shim for vue-excel-editor date cells (legacy vue2-datepicker API).
 */
import DatePickerNext from 'vue-datepicker-next'
import { defineComponent, computed, h } from 'vue'
import { format } from 'date-format-parse'
import en from 'date-format-parse/es/locale/en'
import './inline.css'

const FORMAT_BY_TYPE = {
  date: 'YYYY-MM-DD',
  datetime: 'YYYY-MM-DD HH:mm:ss',
  year: 'YYYY',
  month: 'YYYY-MM',
  time: 'HH:mm:ss',
  week: 'w'
}

function resolveFormat(type, explicitFormat) {
  return explicitFormat || FORMAT_BY_TYPE[type] || FORMAT_BY_TYPE.date
}

function readValue(props) {
  if (props.modelValue !== undefined) return props.modelValue
  return props.value
}

function isInlineProp(inline) {
  return inline === true || inline === ''
}

function normalizePickerValue(external, valueType, fmt) {
  if (valueType === 'date' || valueType === 'timestamp') return external
  if (external instanceof Date && !Number.isNaN(external.getTime())) {
    const outFmt = valueType === 'format' ? fmt : valueType
    return format(external, outFmt, { locale: en })
  }
  if (external == null) return ''
  return typeof external === 'string' ? external : String(external)
}

const DatePicker = defineComponent({
  name: 'DatePicker',
  inheritAttrs: false,
  props: {
    inline: Boolean,
    valueType: {
      type: [String, Object],
      default: 'date'
    },
    modelValue: {},
    value: {},
    type: {
      type: String,
      default: 'date'
    },
    format: String
  },
  emits: ['update:modelValue', 'update:value', 'input', 'change'],
  setup(props, { attrs, emit }) {
    const fmt = computed(() => resolveFormat(props.type, props.format))
    const pickerValue = computed(() => normalizePickerValue(readValue(props), props.valueType, fmt.value))

    function emitValue(value) {
      emit('update:modelValue', value)
      emit('update:value', value)
      emit('input', value)
      emit('change', value)
    }

    return () => {
      const inline = isInlineProp(props.inline)
      const sharedProps = {
        type: props.type,
        format: fmt.value,
        value: pickerValue.value,
        valueType: props.valueType,
        'onUpdate:value': emitValue,
        onChange: emitValue
      }

      if (inline) {
        return h('div', { class: ['mx-datepicker', 'mx-datepicker-inline', 'cat2bug-vue2-datepicker-inline'] }, [
          h(DatePickerNext, {
            ...sharedProps,
            open: true,
            appendToBody: false,
            editable: false
          })
        ])
      }

      return h(DatePickerNext, {
        ...attrs,
        ...sharedProps
      })
    }
  }
})

Object.assign(DatePicker, {
  locale: DatePickerNext.locale,
  install: DatePickerNext.install,
  Calendar: DatePickerNext.Calendar,
  CalendarRange: DatePickerNext.CalendarRange,
  TimePanel: DatePickerNext.TimePanel,
  TimeRange: DatePickerNext.TimeRange,
  DateTime: DatePickerNext.DateTime,
  DateTimeRange: DatePickerNext.DateTimeRange
})

export default DatePicker
