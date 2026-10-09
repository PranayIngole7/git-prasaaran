export type ActivityStatus = 'RECEIVED' | 'PROCESSED' | 'FAILED'

export interface ActivityEvent {
  id: number
  deliveryId: string
  eventType: string
  commitSha: string | null
  status: ActivityStatus
  createdAt: string
  processedAt: string | null
  repositoryId: number | null
}

export interface ActivityPage {
  items: ActivityEvent[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ActivityFilters {
  page?: number
  size?: number
  status?: ActivityStatus
  eventType?: string
  from?: string
  to?: string
}
