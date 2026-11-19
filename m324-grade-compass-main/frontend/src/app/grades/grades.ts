import { HttpErrorResponse } from '@angular/common/http';
import { DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Subject } from '../subjects/subject.model';
import { SubjectService } from '../subjects/subject.service';
import { PASS_GRADE, average } from './grade-average';
import { Grade, MAX_GRADE, MIN_GRADE } from './grade.model';
import { GradeService } from './grade.service';

interface SubjectGroup {
  subject: Subject;
  grades: Grade[];
  average: number | null;
}

@Component({
  selector: 'app-grades',
  imports: [FormsModule, RouterLink, DecimalPipe],
  templateUrl: './grades.html',
  styleUrl: './grades.scss',
})
export class Grades implements OnInit {
  private readonly subjectService = inject(SubjectService);
  private readonly gradeService = inject(GradeService);
  private readonly route = inject(ActivatedRoute);

  protected readonly minGrade = MIN_GRADE;
  protected readonly maxGrade = MAX_GRADE;
  protected readonly passGrade = PASS_GRADE;

  protected readonly subjects = signal<Subject[]>([]);
  protected readonly grades = signal<Grade[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  /** `null`, solange kein Fach gewaehlt ist (z. B. wenn es noch keine Faecher gibt). */
  protected selectedSubjectId: number | null = null;
  /** `null`, solange das Zahlenfeld leer oder ungueltig ist (ngModel liefert dann null). */
  protected newValue: number | null = null;

  // Lint-Demo: explizites `any` (@typescript-eslint/no-explicit-any).
  private lastResponse: any = null;

  protected readonly groups = computed<SubjectGroup[]>(() =>
    this.subjects().map((subject) => {
      const grades = this.grades().filter((grade) => grade.subjectId === subject.id);
      return { subject, grades, average: average(grades.map((grade) => grade.value)) };
    }),
  );

  protected get canAdd(): boolean {
    return this.selectedSubjectId !== null && this.isValidValue(this.newValue);
  }

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    forkJoin({ subjects: this.subjectService.getAll(), grades: this.gradeService.getAll() }).subscribe({
      next: ({ subjects, grades }) => {
        this.subjects.set(subjects);
        this.grades.set(grades);
        this.selectedSubjectId = this.pickSubject(subjects);
        this.error.set(null);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(this.describe(err, 'Noten konnten nicht geladen werden.'));
      },
    });
  }

  protected add(): void {
    // Lint-Demo: `==` statt `===` (eqeqeq).
    if (this.selectedSubjectId == null) {
      return;
    }
    // Lint-Demo: Non-Null-Assertion (@typescript-eslint/no-non-null-assertion).
    const subjectId = this.selectedSubjectId!;
    const value = this.newValue;
    if (!this.isValidValue(value)) {
      this.error.set(`Die Note muss zwischen ${MIN_GRADE} und ${MAX_GRADE} liegen.`);
      return;
    }
    // Lint-Demo: Variable wird nie gelesen (@typescript-eslint/no-unused-vars).
    const unusedCopy = value;

    this.gradeService.create({ subjectId, value }).subscribe({
      next: (created) => {
        this.lastResponse = created;
        // Das Backend schluckt Fehler beim Speichern und liefert dann id 0 statt eines Fehlers.
        if (!created.id) {
          this.error.set('Die Note wurde vom Backend nicht gespeichert.');
          return;
        }
        this.newValue = null;
        this.error.set(null);
        this.reloadGrades();
      },
      error: (err) => this.error.set(this.describe(err, 'Note konnte nicht gespeichert werden.')),
    });
  }

  private reloadGrades(): void {
    this.gradeService.getAll().subscribe({
      next: (grades) => this.grades.set(grades),
      error: (err) => this.error.set(this.describe(err, 'Noten konnten nicht geladen werden.')),
    });
  }

  /** Behaelt die aktuelle Auswahl, sonst `?subject=` aus der URL, sonst das erste Fach. */
  private pickSubject(subjects: Subject[]): number | null {
    const fromUrl = Number(this.route.snapshot.queryParamMap.get('subject'));
    const candidates = [this.selectedSubjectId, fromUrl];
    for (const id of candidates) {
      if (id !== null && subjects.some((subject) => subject.id === id)) {
        return id;
      }
    }
    return subjects[0]?.id ?? null;
  }

  private isValidValue(value: number | null): value is number {
    return value !== null && Number.isFinite(value) && value >= MIN_GRADE && value <= MAX_GRADE;
  }

  private describe(err: unknown, fallback: string): string {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 0) {
        return 'Backend nicht erreichbar.';
      }
      if (err.status === 400) {
        return `Ungültige Note (erlaubt: ${MIN_GRADE} bis ${MAX_GRADE}).`;
      }
    }
    return fallback;
  }
}
