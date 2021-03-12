<template>
  <div class="p-6 flex flex-row">
    <div class="flex-none p-6 max-w-xs w-full relative">
      <button
        class="flex gap-2 px-4 py-1 text-base text-purple-600 font-semibold rounded-full border border-purple-200 hover:text-white hover:bg-purple-600 hover:border-transparent focus:outline-none focus:ring-2 focus:ring-purple-600 focus:ring-offset-2"
        @click="getQR"
      >
        Обновить QR-Code
      </button>

      <div class="mt-1 relative" v-if="qrs.length > 0">
        <label
          id="listbox-label"
          class="block text-sm font-medium text-gray-700"
        >
          Выбранная сеть
        </label>

        <button
          @click="opened = !opened"
          type="button"
          aria-haspopup="listbox"
          aria-expanded="true"
          aria-labelledby="listbox-label"
          class="relative w-full bg-white border border-gray-300 rounded-md shadow-sm pl-3 pr-10 py-2 text-left cursor-default focus:outline-none focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
        >
          <span class="flex items-center">
            <svg
              class="flex-shrink-0 h-6 w-6 rounded-full"
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M8.111 16.404a5.5 5.5 0 017.778 0M12 20h.01m-7.08-7.071c3.904-3.905 10.236-3.905 14.141 0M1.394 9.393c5.857-5.857 15.355-5.857 21.213 0"
              />
            </svg>

            <span class="ml-3 block truncate">
              {{ selectedQR.name }}
            </span>
          </span>

          <span
            class="ml-3 absolute inset-y-0 right-0 flex items-center pr-2 pointer-events-none"
          >
            <svg
              class="h-5 w-5 text-gray-400"
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 20 20"
              fill="currentColor"
              aria-hidden="true"
            >
              <path
                fill-rule="evenodd"
                d="M10 3a1 1 0 01.707.293l3 3a1 1 0 01-1.414 1.414L10 5.414 7.707 7.707a1 1 0 01-1.414-1.414l3-3A1 1 0 0110 3zm-3.707 9.293a1 1 0 011.414 0L10 14.586l2.293-2.293a1 1 0 011.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z"
                clip-rule="evenodd"
              />
            </svg>
          </span>
        </button>

        <div
          v-if="opened"
          class="absolute mt-1 w-full rounded-md bg-white shadow-lg"
        >
          <ul
            tabindex="-1"
            role="listbox"
            aria-labelledby="listbox-label"
            aria-activedescendant="listbox-item-3"
            class="max-h-56 rounded-md py-1 text-base ring-1 ring-black ring-opacity-5 overflow-auto focus:outline-none sm:text-sm"
          >
            <li
              v-for="item in qrs"
              :key="item.name"
              id="listbox-item-0"
              role="option"
              class="text-gray-900 cursor-default select-none relative py-2 pl-3 pr-9 hover:bg-indigo-600 hover:text-white"
              @click="selectQR(item)"
            >
              <div class="flex items-center">
                <img
                  :src="'data:image/png;base64,' + item.qr"
                  class="flex-shrink-0 h-6 w-6 rounded-full"
                />
                <span class="ml-3 block font-normal truncate"
                  >{{ item.name }}
                </span>
              </div>

              <span
                v-if="item.name === selectedQR.name"
                class="absolute inset-y-0 right-0 flex items-center pr-4"
              >
                <svg
                  class="h-5 w-5"
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 20 20"
                  fill="currentColor"
                  aria-hidden="true"
                >
                  <path
                    fill-rule="evenodd"
                    d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                    clip-rule="evenodd"
                  />
                </svg>
              </span>
            </li>
          </ul>
        </div>

        <img :src="selectedQR.qr" />
      </div>
    </div>

    <div
      class="flex-auto p-6 max-w-md max-h-52 bg-white rounded-xl shadow-md flex items-center space-x-4"
    >
      <div>
        <img class="h-12 w-12" src="./assets/logo.png" alt="Re-Star logo" />
      </div>

      <div class="space-y-4">
        <div class="text-xl font-medium text-black">Фото</div>

        <input
          class="px-4 py-1 text-base rounded-full focus:outline-none border border-purple-600"
          type="text"
          v-model="name"
          placeholder="Введите имя товара"
          name=""
        />

        <button
          class="flex gap-2 px-4 py-1 text-base text-purple-600 font-semibold rounded-full border border-purple-200 hover:text-white hover:bg-purple-600 hover:border-transparent focus:outline-none focus:ring-2 focus:ring-purple-600 focus:ring-offset-2"
          @click="snap"
        >
          Снять фото

          <svg
            class="h-6 w-6"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="{2}"
              d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"
            />
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="{2}"
              d="M15 13a3 3 0 11-6 0 3 3 0 016 0z"
            />
          </svg>
        </button>
      </div>
    </div>
  </div>

  <WebRTC />
</template>

<script>
import WebRTC from './components/WebRTC.vue'

export default {
  components: {
    WebRTC,
  },

  data() {
    return {
      counter: 0,
      name: '',
      qrs: [],
      opened: false,
      selectedQR: {
        name: '',
        qr: '',
      },
    }
  },

  methods: {
    selectQR(item) {
      this.selectedQR.name = item.name
      this.selectedQR.qr = 'data:image/png;base64,' + item.qr
      this.opened = false

      localStorage.setItem('currentQRName', item.name)
    },

    async snap() {
      console.log('snap!')
      let resp = await fetch(`http://localhost:5000/snap?name=${this.name}`)
      let data = await resp.text()
      console.log(data)
    },

    async getQR() {
      let resp = await fetch(`http://localhost:5000/qr`)
      let data = await resp.json()
      this.qrs = data
      data.find((el) => {
        if (el.name === this.selectedQR.name) {
          this.selectQR(el)
          return true
        }
      })
    },

    getQRFromLocalStorage() {
      let s = localStorage.getItem('currentQRName')
      if (s !== null) {
        this.selectedQR.name = s
      }
    },
  },
  mounted() {
    this.getQRFromLocalStorage()
    this.getQR()
  },
}
</script>
