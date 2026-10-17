/**
 * ECharts 按需注册。实际图表：柱状、折线、饼、仪表盘、雷达。
 * 新增类型或组件时在此 use()，否则 setOption 静默不渲染对应部分。
 */
import * as echarts from 'echarts/core'
import { BarChart, GaugeChart, LineChart, PieChart, RadarChart } from 'echarts/charts'
import {
  AxisPointerComponent,
  GraphicComponent,
  GridComponent,
  LegendComponent,
  RadarComponent,
  TooltipComponent
} from 'echarts/components'
import { LabelLayout } from 'echarts/features'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  BarChart,
  GaugeChart,
  LineChart,
  PieChart,
  RadarChart,
  AxisPointerComponent,
  GraphicComponent,
  GridComponent,
  LegendComponent,
  RadarComponent,
  TooltipComponent,
  LabelLayout,
  CanvasRenderer
])

export default echarts
