import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TypingTest } from './typing-test';

describe('TypingTest', () => {
  let component: TypingTest;
  let fixture: ComponentFixture<TypingTest>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TypingTest],
    }).compileComponents();

    fixture = TestBed.createComponent(TypingTest);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
