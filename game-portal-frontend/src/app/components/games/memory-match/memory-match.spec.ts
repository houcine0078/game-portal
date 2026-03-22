import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MemoryMatch } from './memory-match';

describe('MemoryMatch', () => {
  let component: MemoryMatch;
  let fixture: ComponentFixture<MemoryMatch>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MemoryMatch],
    }).compileComponents();

    fixture = TestBed.createComponent(MemoryMatch);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
