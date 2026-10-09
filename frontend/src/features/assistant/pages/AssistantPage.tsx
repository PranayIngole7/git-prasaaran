import { Bot, FileText, LoaderCircle, Send } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import {
  askAssistant,
  getAssistantRepositories,
  type AssistantAnswer,
  type AssistantRepository,
} from '../api/assistantApi'

export function AssistantPage() {
  const [repositories, setRepositories] = useState<AssistantRepository[]>([])
  const [repositoryId, setRepositoryId] = useState('')
  const [question, setQuestion] = useState('')
  const [answer, setAnswer] = useState<AssistantAnswer | null>(null)
  const [loadingRepositories, setLoadingRepositories] = useState(true)
  const [asking, setAsking] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let mounted = true

    getAssistantRepositories()
      .then((items) => {
        if (!mounted) return
        const active = items.filter((item) => item.active)
        setRepositories(active)
        if (active.length > 0) setRepositoryId(String(active[0].id))
      })
      .catch(() => {
        if (mounted) setError('Unable to load repositories. Please refresh or sign in again.')
      })
      .finally(() => {
        if (mounted) setLoadingRepositories(false)
      })

    return () => {
      mounted = false
    }
  }, [])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!repositoryId || !question.trim() || asking) return

    setAsking(true)
    setError('')
    setAnswer(null)

    try {
      const result = await askAssistant(Number(repositoryId), question.trim())
      setAnswer(result)
    } catch (caught) {
      const status = (caught as { response?: { status?: number } }).response?.status
      if (status === 503) {
        setError('The AI provider is unavailable. Check the Gemini configuration and try again.')
      } else if (status === 422) {
        setError('No usable Markdown documentation was found in the selected repository.')
      } else if (status === 409) {
        setError('This repository is inactive. Select an active repository.')
      } else {
        setError('Unable to answer this question. Check the backend logs and try again.')
      }
    } finally {
      setAsking(false)
    }
  }

  return (
    <section className="mx-auto max-w-4xl space-y-6">
      <header>
        <div className="mb-2 flex items-center gap-2 text-indigo-700">
          <Bot className="size-6" />
          <span className="text-sm font-semibold">Git-Prasaaran Assistant</span>
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">
          Ask your documentation
        </h1>
        <p className="mt-2 text-sm text-slate-600">
          Ask questions about a registered repository. Answers use its Markdown
          documentation and include the source paths provided to the assistant.
        </p>
      </header>

      <form
        onSubmit={handleSubmit}
        className="space-y-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm"
      >
        <div>
          <label htmlFor="assistant-repository" className="mb-1.5 block text-sm font-medium text-slate-700">
            Repository
          </label>
          <select
            id="assistant-repository"
            value={repositoryId}
            onChange={(event) => {
              setRepositoryId(event.target.value)
              setAnswer(null)
              setError('')
            }}
            disabled={loadingRepositories || repositories.length === 0}
            className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
          >
            {loadingRepositories && <option value="">Loading repositories…</option>}
            {!loadingRepositories && repositories.length === 0 && (
              <option value="">No active repositories available</option>
            )}
            {repositories.map((repository) => (
              <option key={repository.id} value={repository.id}>
                {repository.owner}/{repository.name}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label htmlFor="assistant-question" className="mb-1.5 block text-sm font-medium text-slate-700">
            Question
          </label>
          <textarea
            id="assistant-question"
            value={question}
            onChange={(event) => setQuestion(event.target.value)}
            maxLength={2000}
            rows={4}
            required
            placeholder="How is authentication configured in this repository?"
            className="w-full resize-y rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100"
          />
          <p className="mt-1 text-right text-xs text-slate-500">
            {question.length}/2000
          </p>
        </div>

        <button
          type="submit"
          disabled={asking || !repositoryId || !question.trim() || loadingRepositories}
          className="inline-flex items-center gap-2 rounded-md bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {asking ? <LoaderCircle className="size-4 animate-spin" /> : <Send className="size-4" />}
          {asking ? 'Thinking…' : 'Ask assistant'}
        </button>
      </form>

      {error && (
        <div role="alert" className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
          {error}
        </div>
      )}

      {answer && (
        <article className="space-y-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">Answer</h2>
            <p className="mt-1 text-xs text-slate-500">Provider model: {answer.model}</p>
          </div>
          <div className="whitespace-pre-wrap break-words text-sm leading-7 text-slate-700">
            {answer.answer}
          </div>
          <div className="border-t border-slate-100 pt-4">
            <h3 className="mb-2 flex items-center gap-2 text-sm font-semibold text-slate-800">
              <FileText className="size-4" />
              Documentation context
            </h3>
            <ul className="space-y-1">
              {answer.sources.map((source) => (
                <li key={source} className="break-all font-mono text-xs text-slate-600">
                  {source}
                </li>
              ))}
            </ul>
          </div>
        </article>
      )}
    </section>
  )
}
