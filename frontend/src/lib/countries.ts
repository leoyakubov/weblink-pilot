const globe = '\u{1F310}'
const house = '\u{1F3E0}'
const question = '\u{2753}'

export function countryFlag(code: string) {
  const value = code.trim().toUpperCase()
  if (!value) {
    return globe
  }
  if (value === 'LOCAL') {
    return house
  }
  if (value === 'UNKNOWN') {
    return question
  }
  if (!/^[A-Z]{2}$/.test(value)) {
    return globe
  }

  const [first, second] = value
  return String.fromCodePoint(
    0x1f1e6 + first.charCodeAt(0) - 65,
    0x1f1e6 + second.charCodeAt(0) - 65,
  )
}

export function countryLabel(code: string) {
  const value = code.trim().toUpperCase()
  return `${countryFlag(value)} ${value}`
}
