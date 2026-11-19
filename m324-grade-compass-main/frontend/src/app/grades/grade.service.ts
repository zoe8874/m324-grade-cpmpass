import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { Grade, GradeRequest } from './grade.model';

/**
 * Das Backend bietet fuer Noten nur `GET /api/grades` und `POST /api/grades` an.
 * Es gibt bewusst weder `update` noch `delete`, solange dafuer kein Endpunkt existiert
 * (CORS erlaubt fuer Noten zwar PUT/DELETE, der Controller kennt sie aber nicht -> 405).
 */
@Injectable({ providedIn: 'root' })
export class GradeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl.replace(/\/+$/, '')}/api/grades`;

  getAll(): Observable<Grade[]> {
    return this.http.get<Partial<Grade>[]>(this.baseUrl).pipe(map((grades) => grades.filter(isGrade)));
  }

  create(request: GradeRequest): Observable<Grade> {
    return this.http.post<Grade>(this.baseUrl, request);
  }
}

/** Verwirft Eintraege mit fehlenden oder nicht-numerischen Feldern. */
function isGrade(grade: Partial<Grade> | null): grade is Grade {
  return (
    grade !== null &&
    typeof grade.id === 'number' &&
    typeof grade.subjectId === 'number' &&
    typeof grade.value === 'number' &&
    Number.isFinite(grade.value)
  );
}
