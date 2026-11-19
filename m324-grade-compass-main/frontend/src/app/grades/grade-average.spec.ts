import { average } from './grade-average';

describe('average', () => {
  it('returns null without grades', () => {
    expect(average([])).toBeNull();
  });

  it('returns the mean rounded to two decimals', () => {
    expect(average([4, 5, 5.5])).toBe(4.83);
  });
});
