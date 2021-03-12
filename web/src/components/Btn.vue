<template>
  <button
    class="disabled:opacity-50 px-4 py-1 text-base text-purple-600 font-semibold rounded-full border border-purple-200 hover:text-white hover:bg-purple-600 hover:border-transparent focus:outline-none focus:ring-2 focus:ring-purple-600 focus:ring-offset-2"
  >
    {{ value }}
  </button>
</template>

<script>
export default {
  props: {
    value: String,
  },
  name: 'Btn',
}
</script>
