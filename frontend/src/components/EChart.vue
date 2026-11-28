<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { ECharts } from 'echarts/core'
import type { EChartsOption } from 'echarts'

echarts.use([BarChart, LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{
  option: EChartsOption
  height?: string
}>()

const element = ref<HTMLDivElement | null>(null)
let chart: ECharts | null = null
let observer: ResizeObserver | null = null

function render() {
  chart?.setOption(props.option, true)
}

onMounted(() => {
  if (!element.value) return
  chart = echarts.init(element.value)
  render()
  observer = new ResizeObserver(() => chart?.resize())
  observer.observe(element.value)
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div ref="element" :style="{ width: '100%', height: height || '320px' }" />
</template>
