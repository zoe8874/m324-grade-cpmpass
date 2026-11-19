/** Einfacher (ungewichteter) Durchschnitt, `null` wenn es keine Noten gibt. Auf 2 Stellen gerundet. */
export function average(values: readonly number[]): number | null {
  if (values.length === 0) {
    return null;
  }
  const sum = values.reduce((total, value) => total + value, 0);
  return Math.round((sum / values.length) * 100) / 100;
}

/** Schweizer Bestehensgrenze: ab 4.0 ist ein Fach genuegend. */
export const PASS_GRADE = 4;
