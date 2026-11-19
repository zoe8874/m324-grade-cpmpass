import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { Subject } from './subject.model';
import { SubjectService } from './subject.service';

@Component({
  selector: 'app-subjects',
  imports: [FormsModule],
  templateUrl: './subjects.html',
  styleUrl: './subjects.scss',
})
export class Subjects implements OnInit {
  private readonly subjectService = inject(SubjectService);

  protected readonly subjects = signal<Subject[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected newName = '';
  protected editingId = signal<number | null>(null);
  protected editName = '';

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.subjectService.getAll().subscribe({
      next: (subjects) => {
        this.subjects.set(subjects);
        this.error.set(null);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(this.describe(err, 'Fächer konnten nicht geladen werden.'));
      },
    });
  }

  protected add(): void {
    const name = this.newName.trim();
    if (!name) {
      return;
    }
    this.run(this.subjectService.create({ name }), 'Fach konnte nicht angelegt werden.', () => {
      this.newName = '';
    });
  }

  protected startEdit(subject: Subject): void {
    this.editingId.set(subject.id);
    this.editName = subject.name;
  }

  protected cancelEdit(): void {
    this.editingId.set(null);
  }

  protected saveEdit(subject: Subject): void {
    const name = this.editName.trim();
    if (!name) {
      return;
    }
    this.run(
      this.subjectService.update(subject.id, { name }),
      'Fach konnte nicht gespeichert werden.',
      () => this.editingId.set(null),
    );
  }

  protected remove(subject: Subject): void {
    if (!confirm(`Fach "${subject.name}" wirklich löschen?`)) {
      return;
    }
    this.run(this.subjectService.delete(subject.id), 'Fach konnte nicht gelöscht werden.');
  }

  /** Runs a mutation, then reloads the list. */
  private run(request: Observable<unknown>, fallback: string, onSuccess?: () => void): void {
    request.subscribe({
      next: () => {
        onSuccess?.();
        this.load();
      },
      error: (err) => this.error.set(this.describe(err, fallback)),
    });
  }

  private describe(err: unknown, fallback: string): string {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 0) {
        return 'Backend nicht erreichbar.';
      }
      if (err.status === 409) {
        return 'Ein Fach mit diesem Namen existiert bereits.';
      }
      if (err.status === 400) {
        return 'Ungültiger Name (nicht leer, max. 100 Zeichen).';
      }
    }
    return fallback;
  }
}
