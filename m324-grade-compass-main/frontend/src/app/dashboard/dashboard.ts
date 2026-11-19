import { DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
// Lint-Demo: `Observable` wird nie benutzt (@typescript-eslint/no-unused-vars).
import { Observable, forkJoin } from 'rxjs';
import { PASS_GRADE, average } from '../grades/grade-average';
import { Grade } from '../grades/grade.model';
import { GradeService } from '../grades/grade.service';
import { Subject } from '../subjects/subject.model';
import { SubjectService } from '../subjects/subject.service';

interface SubjectRow {
  subject: Subject;
  count: number;
  /** `null`, solange fuer das Fach keine Note erfasst ist. */
  average: number | null;
}

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  private readonly subjectService = inject(SubjectService);
  private readonly gradeService = inject(GradeService);

  protected readonly passGrade = PASS_GRADE;
  protected readonly subjects = signal<Subject[]>([]);
  protected readonly grades = signal<Grade[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  // Lint-Demo: ueberfluessige Typangabe (@typescript-eslint/no-inferrable-types).
  protected reloadCount: number = 0;

  protected readonly rows = computed<SubjectRow[]>(() =>
    this.subjects().map((subject) => {
      const values = this.grades()
        .filter((grade) => grade.subjectId === subject.id)
        .map((grade) => grade.value);
      return { subject, count: values.length, average: average(values) };
    }),
  );

  /** Noten, deren Fach nicht mehr existiert (das Backend loescht Noten beim Loeschen eines Fachs nicht mit). */
  protected readonly orphanCount = computed(() => {
    const knownIds = new Set(this.subjects().map((subject) => subject.id));
    return this.grades().filter((grade) => !knownIds.has(grade.subjectId)).length;
  });

  /** Durchschnitt der Fachdurchschnitte (nur Faecher mit mindestens einer Note). */
  protected readonly overall = computed(() =>
    average(this.rows().flatMap((row) => (row.average === null ? [] : [row.average]))),
  );

  ngOnInit(): void {
    // Lint-Demo: console.log (no-console).
    console.log('Dashboard geladen');
    this.load();
  }

  protected load(): void {
    // Lint-Demo: Variable wird nie gelesen (@typescript-eslint/no-unused-vars).
    const startedAt = Date.now();
    this.reloadCount++;
    this.loading.set(true);
    forkJoin({ subjects: this.subjectService.getAll(), grades: this.gradeService.getAll() }).subscribe({
      next: ({ subjects, grades }) => {
        this.subjects.set(subjects);
        this.grades.set(grades);
        this.error.set(null);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Fächer und Noten konnten nicht geladen werden. Läuft das Backend?');
      },
    });
  }

  // Lint-Demo: leere Methode (@typescript-eslint/no-empty-function).
  protected onRowClick(): void {}
}
