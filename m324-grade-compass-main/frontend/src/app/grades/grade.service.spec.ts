import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Grade } from './grade.model';
import { GradeService } from './grade.service';

describe('GradeService', () => {
  let service: GradeService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(GradeService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('drops entries with missing or non-numeric fields', () => {
    let result: Grade[] = [];
    service.getAll().subscribe((grades) => (result = grades));

    http.expectOne('http://localhost:8080/api/grades').flush([
      { id: 1, subjectId: 2, value: 5 },
      { id: 2, subjectId: 2, value: null },
      { id: 3, value: 4 },
      null,
    ]);

    expect(result).toEqual([{ id: 1, subjectId: 2, value: 5 }]);
  });

  it('posts a new grade', () => {
    service.create({ subjectId: 2, value: 5.5 }).subscribe();

    const req = http.expectOne('http://localhost:8080/api/grades');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ subjectId: 2, value: 5.5 });
    req.flush({ id: 1, subjectId: 2, value: 5.5 });
  });
});
