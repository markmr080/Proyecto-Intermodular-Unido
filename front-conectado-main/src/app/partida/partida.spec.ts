import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Crearpartida } from './crearpartida';

describe('Crearpartida', () => {
  let component: Crearpartida;
  let fixture: ComponentFixture<Crearpartida>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Crearpartida]
    })
      .compileComponents();

    fixture = TestBed.createComponent(Crearpartida);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
