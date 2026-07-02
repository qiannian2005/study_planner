<template>
  <div class="card h-100">
    <div class="card-header d-flex justify-content-between align-items-center bg-white">
      <h6 class="mb-0"><i class="bi bi-bar-chart-line"></i> {{ $t('studyChart.title') }}</h6>
      <div class="btn-group btn-group-sm">
        <button 
          type="button" 
          class="btn btn-outline-primary" 
          :class="{ active: period === 'week' }"
          @click="period = 'week'"
        >
          {{ $t('studyChart.thisWeek') }}
        </button>
        <button 
          type="button" 
          class="btn btn-outline-primary" 
          :class="{ active: period === 'month' }"
          @click="period = 'month'"
        >
          {{ $t('studyChart.thisMonth') }}
        </button>
      </div>
    </div>
    <div class="card-body">
      <div ref="chartRef" style="width: 100%; height: 300px;"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted, toRefs } from 'vue'
import { useI18n } from 'vue-i18n'
import * as echarts from 'echarts'

const { t } = useI18n()

const props = defineProps({
  chartData: {
    type: Object,
    default: () => ({
      week: { xAxis: [], series: [] },
      month: { xAxis: [], series: [] }
    })
  }
})

const { chartData } = toRefs(props)
const chartRef = ref(null)
let chartInstance = null
const period = ref('week') // 'week' or 'month'

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
  }
  window.removeEventListener('resize', handleResize)
})

watch([period, chartData], () => {
  updateChart()
}, { deep: true })

function initChart() {
  if (chartRef.value) {
    chartInstance = echarts.init(chartRef.value)
    updateChart()
  }
}

function handleResize() {
  if (chartInstance) {
    chartInstance.resize()
  }
}

function updateChart() {
  if (!chartInstance) return

  const isWeek = period.value === 'week'
  const data = isWeek ? chartData.value.week : chartData.value.month
  
  // 如果没有数据，显示空状态或默认坐标轴
  const xAxisData = data?.xAxis?.length ? data.xAxis : []
  const seriesData = data?.series?.length ? data.series : []
  
  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const point = Array.isArray(params) ? params[0] : params
        return `${point?.name ?? ''} <br/> ${t('studyChart.studyTime')}: ${point?.value ?? 0} ${t('studyChart.hours')}`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xAxisData
    },
    yAxis: {
      type: 'value',
      name: t('studyChart.hours')
    },
    series: [
      {
        name: t('studyChart.studyTime'),
        type: 'line',
        smooth: true,
        data: seriesData,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(62, 141, 247, 0.42)' },
            { offset: 1, color: 'rgba(74, 154, 114, 0.08)' }
          ])
        },
        itemStyle: {
          color: '#3e8df7'
        },
        lineStyle: {
          color: '#3e8df7',
          width: 3
        }
      }
    ]
  }

  chartInstance.setOption(option)
}
</script>

<style scoped>
.card {
  border-radius: 8px;
}
</style>
