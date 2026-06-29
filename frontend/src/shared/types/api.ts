export interface ApiSettings {
  apiBaseUrl: string;
  authToken: string;
  refreshToken: string;
}

export interface CreateLinkRequest {
  originalUrl: string;
  customAlias?: string | null;
  expiresAt?: string | null;
}

export interface LinkResponse {
  code: string;
  shortUrl: string;
  qrCodeUrl: string;
  originalUrl: string;
  createdAt: string;
  expiresAt: string | null;
  clickCount: number;
  ownerUsername: string | null;
  ownerRole?: string | null;
  aiMetadata?: AiLinkMetadataResponse | null;
}

export interface AiLinkMetadataResponse {
  code: string;
  status: string;
  provider: string;
  promptVersion: string;
  title: string | null;
  summary: string | null;
  category: string | null;
  tags: string[];
  icon: string | null;
  suggestedAlias: string | null;
  errorMessage: string | null;
  updatedAt: string | null;
  completedAt: string | null;
}

export interface LinkCreatorOptionResponse {
  username: string;
  role: string;
}

export interface AuthCredentialsRequest {
  username: string;
  password: string;
  email?: string | null;
}

export interface AuthResponse {
  token: string;
  username: string;
  role: string;
}

export interface AccountActionPreviewResponse {
  previewLink: string | null;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface PasswordResetRequest {
  email: string;
}

export interface PasswordResetConfirmRequest {
  token: string;
  password: string;
}

export interface PasswordChangeRequest {
  currentPassword: string;
  newPassword: string;
}

export interface EmailVerificationRequest {
  email: string;
}

export interface EmailVerificationConfirmRequest {
  token: string;
}

export interface OAuthLoginCompleteRequest {
  ticket: string;
}

export interface UserProfileResponse {
  username: string;
  role: string;
}

export interface AccountIdentityResponse {
  provider: string;
  providerLogin: string;
}

export interface AccountProfileResponse {
  username: string;
  role: string;
  email: string | null;
  emailVerified: boolean;
  createdAt: string;
  lastLoginAt: string | null;
  socialIdentities: AccountIdentityResponse[];
}

export interface AdminOverviewResponse {
  totalUsers: number;
  adminUsers: number;
  totalLinks: number;
  anonymousLinks: number;
  ownedLinks: number;
  totalClicks: number;
}

export interface AdminUserResponse {
  username: string;
  email: string | null;
  role: string;
  enabled: boolean;
  emailVerified: boolean;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface AdminRuntimeMetricResponse {
  group: string;
  name: string;
  value: string;
  unit: string;
  description: string;
}

export interface AdminHealthComponentResponse {
  name: string;
  status: string;
  detail: string;
}

export interface AdminConfigurationItemResponse {
  name: string;
  value: string;
  description: string;
}

export interface AdminMonitoringResponse {
  metrics: AdminRuntimeMetricResponse[];
  health: AdminHealthComponentResponse[];
  configuration: AdminConfigurationItemResponse[];
}

export interface RedirectPreviewResponse {
  code: string;
  shortUrl: string;
  targetUrl: string;
  status: number;
  locationHeader: string;
}

export interface AnalyticsCountryStat {
  country: string;
  clicks: number;
}

export interface AnalyticsSummaryResponse {
  code: string;
  totalClicks: number;
  redirectClicks: number;
  qrScans: number;
  uniqueVisitors: number;
  lastClickedAt: string | null;
  lastReferrer: string | null;
  lastBrowserFamily: string | null;
  lastDeviceType: string | null;
  topCountries: AnalyticsCountryStat[];
}

export interface AnalyticsBucketStat {
  bucket: string;
  totalClicks: number;
  redirectClicks: number;
  qrScans: number;
  uniqueVisitors: number;
}

export interface AnalyticsBreakdownStat {
  label: string;
  clicks: number;
}

export interface AnalyticsEventResponse {
  clickedAt: string | null;
  eventSource: 'REDIRECT' | 'QR_SCAN';
  referrer: string | null;
  country: string | null;
  browserFamily: string | null;
  deviceType: string | null;
}

export interface AnalyticsDetailsResponse {
  code: string;
  timelineByDay: AnalyticsBucketStat[];
  timelineByHour: AnalyticsBucketStat[];
  browserBreakdown: AnalyticsBreakdownStat[];
  deviceBreakdown: AnalyticsBreakdownStat[];
  referrerBreakdown: AnalyticsBreakdownStat[];
  recentEvents: AnalyticsEventResponse[];
  sourceTrendByDay: AnalyticsBucketStat[];
  visitorTrendByDay: AnalyticsBucketStat[];
}
