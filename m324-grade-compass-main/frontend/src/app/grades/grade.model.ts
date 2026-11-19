/**
 * Entspricht `GradeResponse` im Backend (Long id, Long subjectId, Double value).
 * Die Backend-Felder sind boxed Typen, werden aber nie bewusst auf null gesetzt.
 * Der Service filtert unbrauchbare Eintraege trotzdem raus (siehe `GradeService.getAll`).
 */
export interface Grade {
  id: number;
  subjectId: number;
  value: number;
}

/** Entspricht `CreateGradeRequest`: beide Felder sind im Backend `@NotNull`. */
export interface GradeRequest {
  subjectId: number;
  value: number;
}

/** Backend-Validierung in `GradeService.create`: 1 <= value <= 6. */
export const MIN_GRADE = 1;
export const MAX_GRADE = 6;
