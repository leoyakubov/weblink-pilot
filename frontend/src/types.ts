export interface ApiSettings {
  apiBaseUrl: string
  username: string
  password: string
}

export interface CreateLinkRequest {
  originalUrl: string
  customAlias?: string | null
  expiresAt?: string | null
}

export interface LinkResponse {
  code: string
  shortUrl: string
  qrCodeUrl: string
  originalUrl: string
  createdAt: string
  expiresAt: string | null
  clickCount: number
}

export interface RedirectPreviewResponse {
  code: string
  shortUrl: string
  targetUrl: string
  status: number
  locationHeader: string
}

export interface AnalyticsCountryStat {
  country: string
  clicks: number
}

export interface AnalyticsSummaryResponse {
  code: string
  totalClicks: number
  redirectClicks: number
  qrScans: number
  uniqueVisitors: number
  lastClickAt: string | null
  lastReferrer: string | null
  lastBrowserFamily: string | null
  lastDeviceType: string | null
  topCountries: AnalyticsCountryStat[]
}
