/**
 * ECharts 按需注册入口；新增图表类型或组件时需在此补充 use()，否则 setOption 静默不渲染对应部分。
 */
import * as echarts from 'echarts/core'
import { BarChart, GaugeChart, LineChart, PieChart, RadarChart, ScatterChart } from 'echarts/charts'
import {
  AxisPointerComponent,
  DataZoomComponent,
  DatasetComponent,
  GraphicComponent,
  GridComponent,
  LegendComponent,
  MarkAreaComponent,
  MarkLineComponent,
  MarkPointComponent,
  RadarComponent,
  TitleComponent,
  ToolboxComponent,
  TooltipComponent,
  VisualMapComponent
} from 'echarts/components'
import { LabelLayout, UniversalTransition } from 'echarts/features'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  BarChart,
  GaugeChart,
  LineChart,
  PieChart,
  RadarChart,
  ScatterChart,
  AxisPointerComponent,
  DataZoomComponent,
  DatasetComponent,
  GraphicComponent,
  GridComponent,
  LegendComponent,
  MarkAreaComponent,
  MarkLineComponent,
  MarkPointComponent,
  RadarComponent,
  TitleComponent,
  ToolboxComponent,
  TooltipComponent,
  VisualMapComponent,
  LabelLayout,
  UniversalTransition,
  CanvasRenderer
])

export default echarts
