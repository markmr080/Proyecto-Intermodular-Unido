import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SeleccionPersonajesComponent } from './seleccion-personajes.component';

describe('SeleccionPersonajesComponent', () => {
  let component: SeleccionPersonajesComponent;
  let fixture: ComponentFixture<SeleccionPersonajesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SeleccionPersonajesComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(SeleccionPersonajesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
