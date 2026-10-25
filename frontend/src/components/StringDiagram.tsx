import type { StringStep } from '../api/exercises'

// 본문·패턴 비교 위치와 문자열 변환 결과 표시
export default function StringDiagram({ step }: { step: StringStep }) {
    const activeIndex = (step.offset ?? 0) + (step.patternIndex ?? 0)
    return (
        <div className="string-diagram">
            <div className="string-scroll">
                <div className="string-label">본문</div>
                <div className="string-row" aria-label="본문 문자 배열">
                    {[...step.text].map((character, index) => (
                        <span className="string-cell" data-active={step.pattern != null && activeIndex === index} key={index}>
                            <small>[{index}]</small><strong>{character === ' ' ? '␣' : character}</strong>
                        </span>
                    ))}
                    {step.text.length === 0 && <span>빈 문자열</span>}
                </div>
                {step.pattern != null && (
                    <>
                        <div className="string-label">패턴 · 시작 위치 {step.offset}</div>
                        <div className="string-row" aria-label="패턴 문자 배열" style={{ paddingLeft: (step.offset ?? 0) * 36 }}>
                            {[...step.pattern].map((character, index) => (
                                <span className="string-cell" data-active={step.patternIndex === index} key={index}>
                                    <small>[{index}]</small><strong>{character === ' ' ? '␣' : character}</strong>
                                </span>
                            ))}
                            {step.pattern.length === 0 && <span>빈 패턴</span>}
                        </div>
                    </>
                )}
            </div>
            {step.tokens && (
                <ol className="string-tokens" aria-label="토큰 배열">
                    {step.tokens.map((token, index) => <li key={index}><small>[{index}]</small><code>{JSON.stringify(token)}</code></li>)}
                </ol>
            )}
            {step.output != null && <p className="string-result">결과 <code>{JSON.stringify(step.output)}</code></p>}
        </div>
    )
}
