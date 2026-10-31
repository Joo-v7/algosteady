// 현재 페이지 주변의 번호를 양 끝 범위에 맞춰 선택
export function getPageNumbers(page: number, totalPages: number, visibleCount: number): number[] {
    const count = Math.min(totalPages, visibleCount)
    const start = Math.max(1, Math.min(page - Math.floor(count / 2), totalPages - count + 1))
    return Array.from({ length: count }, (_, index) => start + index)
}
