import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
    plugins: [react()],
    server: {
        port: 5173,
        strictPort: true, // 포트가 사용 중이면 다른 포트로 전환하지 않는다.
        proxy: {
            '/api': {
                target: 'http://localhost:8090',
            },
        },
    },
})
