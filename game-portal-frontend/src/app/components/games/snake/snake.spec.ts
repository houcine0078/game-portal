import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SnakeComponent } from './snake'; // <-- Fixed import!

describe('SnakeComponent', () => {
  let component: SnakeComponent;
  let fixture: ComponentFixture<SnakeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SnakeComponent] // <-- Fixed component name!
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(SnakeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});