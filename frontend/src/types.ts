export interface ApiSettings {
  apiBaseUrl: string
  authToken: string
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
  ownerUsername: string | null
}

export interface AuthCredentialsRequest {
  username: string
  password: string
}

export interface AuthResponse {
  token: string
  username: string
  role: string
}

export interface UserProfileResponse {
  username: string
  role: string
}

export interface AdminOverviewResponse {
  totalUsers: number
  adminUsers: number
  totalLinks: number
  anonymousLinks: number
  ownedLinks: number
  totalClicks: number
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
  lastClickedAt: string | null
  lastReferrer: string | null
  lastBrowserFamily: string | null
  lastDeviceType: string | null
  topCountries: AnalyticsCountryStat[]
}
