const FLAG_CDN_BASE = 'https://flagcdn.com/w40'

export function countryCodeLabel(code: string) {
  return code.trim().toUpperCase()
}

export function countryFlagUrl(code: string) {
  const value = countryCodeLabel(code)
  return /^[A-Z]{2}$/.test(value) ? `${FLAG_CDN_BASE}/${value.toLowerCase()}.png` : null
}
