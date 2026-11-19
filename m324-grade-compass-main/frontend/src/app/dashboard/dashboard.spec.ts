import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  it('shows the average per subject, a dash without grades and warns about orphan grades', async () => {
    const fixture = TestBed.createComponent(Dashboard);
    fixture.detectChanges();

    http.expectOne('http://localhost:8080/api/subjects').flush([
      { id: 1, name: 'Mathe' },
      { id: 2, name: 'Sport' },
    ]);
    http.expectOne('http://localhost:8080/api/grades').flush([
      { id: 1, subjectId: 1, value: 4 },
      { id: 2, subjectId: 1, value: 5 },
      { id: 3, subjectId: 99, value: 6 },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();

    const rows = Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll('tbody tr')).map(
      (row) => Array.from(row.querySelectorAll('td'), (cell) => cell.textContent?.trim()),
    );
    expect(rows).toEqual([
      ['Mathe', '2', '4.50'],
      ['Sport', '0', '–'],
    ]);
    expect(fixture.nativeElement.querySelector('.warning')?.textContent).toContain('1 Note(n)');
  });
});
