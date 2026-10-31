package com.ipt.siga_backend.config;

import com.ipt.siga_backend.dto.CareerRequest;
import com.ipt.siga_backend.dto.StudyPlanRequest;
import com.ipt.siga_backend.dto.SubjectRequest;
import com.ipt.siga_backend.dto.StudentRequest;
import com.ipt.siga_backend.dto.TeacherRequest;
import com.ipt.siga_backend.entity.Career;
import com.ipt.siga_backend.entity.StudyPlan;
import com.ipt.siga_backend.repository.CareerRepository;
import com.ipt.siga_backend.service.CareerService;
import com.ipt.siga_backend.service.StudyPlanService;
import com.ipt.siga_backend.service.SubjectService;
import com.ipt.siga_backend.service.TeacherService;
import com.ipt.siga_backend.service.StudentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// Loads Appendix A (careers, study plans and subjects) on startup, only if the DB is empty
@Component
public class DataLoader implements CommandLineRunner {

    private final CareerRepository careerRepository;
    private final CareerService careerService;
    private final StudyPlanService studyPlanService;
    private final SubjectService subjectService;
    private final TeacherService teacherService;
    private final StudentService studentService;
    private final boolean loadData;

    public DataLoader(CareerRepository careerRepository, CareerService careerService,
                      StudyPlanService studyPlanService, SubjectService subjectService,
                      TeacherService teacherService, StudentService studentService,
                      @Value("${app.load-data}") boolean loadData) {
        this.careerRepository = careerRepository;
        this.careerService = careerService;
        this.studyPlanService = studyPlanService;
        this.subjectService = subjectService;
        this.teacherService = teacherService;
        this.studentService = studentService;
        this.loadData = loadData;
    }

    @Override
    public void run(String... args) {
        // Only one replica loads data (APP_LOAD_DATA), and only if the DB is empty
        if (!loadData || careerRepository.count() > 0) {
            return;
        }
        loadAnalystProgrammer();
        loadDataAnalysis();
        loadCybersecurity();
        loadDemoPeople();
    }

    // Demo teachers and student: same emails as the Keycloak users (teacher1, teacher2, student1)
    private void loadDemoPeople() {
        teacherService.createTeacher(new TeacherRequest("15432876-5", "María González", "maria.gonzalez@ipt.cl",
                "Ingeniera Civil en Informática", "Magíster en Ingeniería de Software"));
        teacherService.createTeacher(new TeacherRequest("13987654-2", "Juan Pérez", "juan.perez@ipt.cl",
                "Ingeniero en Computación", "Magíster en Ciencias de la Computación"));
        Long analystCareerId = careerRepository.findByCode("10450").orElseThrow().getId();
        studentService.createStudent(new StudentRequest("19234123-4", "Pedro Soto", "pedro.soto@ipt.cl",
                analystCareerId));
    }

    // 10450 - Técnico Analista Programador, plan 2021.3
    private void loadAnalystProgrammer() {
        Long planId = createCareerWithPlan("10450", "Técnico Analista Programador",
                "Carrera técnica de dos años orientada al desarrollo de software", "2021.3");
        // Semester 1
        addSubject(planId, "TAP101", "Fundamentos de Programación", 1, 2, 2, 2, 6);
        addSubject(planId, "TAP102", "Matemática Aplicada", 1, 2, 2, 0, 5);
        addSubject(planId, "TAP103", "Fundamentos de Computación", 1, 2, 0, 2, 4);
        addSubject(planId, "TAP104", "Bases de Datos I", 1, 2, 0, 2, 5);
        addSubject(planId, "TAP105", "Comunicación Efectiva", 1, 2, 2, 0, 4);
        addSubject(planId, "TAP106", "Inglés Técnico I", 1, 2, 2, 0, 4);
        // Semester 2
        addSubject(planId, "TAP201", "Programación Orientada a Objetos", 2, 2, 2, 2, 6, "TAP101");
        addSubject(planId, "TAP202", "Estructuras de Datos", 2, 2, 2, 2, 6, "TAP101");
        addSubject(planId, "TAP203", "Bases de Datos II", 2, 2, 0, 2, 5, "TAP104");
        addSubject(planId, "TAP204", "Desarrollo Web I", 2, 2, 0, 4, 6, "TAP101");
        addSubject(planId, "TAP205", "Sistemas Operativos", 2, 2, 0, 2, 4, "TAP103");
        addSubject(planId, "TAP206", "Inglés Técnico II", 2, 2, 2, 0, 3, "TAP106");
        // Semester 3
        addSubject(planId, "TAP301", "Desarrollo Web II", 3, 2, 0, 4, 6, "TAP201", "TAP204");
        addSubject(planId, "TAP302", "Ingeniería de Software", 3, 2, 2, 0, 5, "TAP201");
        addSubject(planId, "TAP303", "Desarrollo de Aplicaciones Móviles", 3, 2, 0, 4, 6, "TAP201");
        addSubject(planId, "TAP304", "Administración de Bases de Datos", 3, 2, 0, 2, 5, "TAP203");
        addSubject(planId, "TAP305", "Redes y Comunicaciones", 3, 2, 0, 2, 4, "TAP205");
        addSubject(planId, "TAP306", "Taller de Programación", 3, 0, 0, 4, 4, "TAP201", "TAP202");
        // Semester 4
        addSubject(planId, "TAP401", "Arquitectura y Desarrollo de Software", 4, 2, 0, 4, 6, "TAP301", "TAP302");
        addSubject(planId, "TAP402", "Desarrollo de Aplicaciones Cloud", 4, 2, 0, 4, 6, "TAP301");
        addSubject(planId, "TAP403", "Seguridad de Aplicaciones", 4, 2, 0, 2, 4, "TAP301", "TAP305");
        addSubject(planId, "TAP404", "Calidad y Pruebas de Software", 4, 2, 0, 2, 5, "TAP302");
        addSubject(planId, "TAP405", "Proyecto de Desarrollo de Software", 4, 0, 0, 6, 6, "TAP301", "TAP302", "TAP306");
        addSubject(planId, "TAP406", "Emprendimiento y Gestión de Proyectos", 4, 2, 2, 0, 3, "TAP302");
    }

    // 10451 - Técnico en Análisis de Datos, plan 2022.3
    private void loadDataAnalysis() {
        Long planId = createCareerWithPlan("10451", "Técnico en Análisis de Datos",
                "Carrera técnica de dos años orientada al análisis de datos", "2022.3");
        // Semester 1
        addSubject(planId, "TAD101", "Fundamentos de Programación", 1, 2, 2, 2, 6);
        addSubject(planId, "TAD102", "Matemática Aplicada", 1, 2, 2, 0, 5);
        addSubject(planId, "TAD103", "Introducción al Análisis de Datos", 1, 2, 0, 2, 5);
        addSubject(planId, "TAD104", "Bases de Datos I", 1, 2, 0, 2, 5);
        addSubject(planId, "TAD105", "Herramientas Computacionales", 1, 0, 0, 4, 4);
        addSubject(planId, "TAD106", "Comunicación Efectiva", 1, 2, 2, 0, 4);
        // Semester 2
        addSubject(planId, "TAD201", "Programación para Análisis de Datos", 2, 2, 0, 4, 6, "TAD101");
        addSubject(planId, "TAD202", "Probabilidad y Estadística", 2, 2, 2, 2, 6, "TAD102");
        addSubject(planId, "TAD203", "Bases de Datos II", 2, 2, 0, 2, 5, "TAD104");
        addSubject(planId, "TAD204", "Preparación y Calidad de Datos", 2, 2, 0, 4, 5, "TAD103", "TAD101");
        addSubject(planId, "TAD205", "Visualización de Datos", 2, 2, 0, 2, 5, "TAD103");
        addSubject(planId, "TAD206", "Inglés Técnico", 2, 2, 2, 0, 3);
        // Semester 3
        addSubject(planId, "TAD301", "Estadística Aplicada", 3, 2, 2, 2, 6, "TAD202");
        addSubject(planId, "TAD302", "Minería de Datos", 3, 2, 0, 4, 6, "TAD201", "TAD202");
        addSubject(planId, "TAD303", "Inteligencia de Negocios", 3, 2, 0, 4, 5, "TAD203", "TAD205");
        addSubject(planId, "TAD304", "Integración y Procesamiento de Datos", 3, 2, 0, 4, 5, "TAD201", "TAD203");
        addSubject(planId, "TAD305", "Visualización Avanzada de Datos", 3, 2, 0, 2, 4, "TAD205");
        addSubject(planId, "TAD306", "Ética y Protección de Datos", 3, 2, 2, 0, 4, "TAD103");
        // Semester 4
        addSubject(planId, "TAD401", "Fundamentos de Machine Learning", 4, 2, 0, 4, 6, "TAD301", "TAD302");
        addSubject(planId, "TAD402", "Big Data y Procesamiento de Datos", 4, 2, 0, 4, 5, "TAD304");
        addSubject(planId, "TAD403", "Analítica de Negocios", 4, 2, 0, 2, 5, "TAD301", "TAD303");
        addSubject(planId, "TAD404", "Desarrollo de Soluciones de Datos", 4, 2, 0, 4, 5, "TAD302", "TAD304");
        addSubject(planId, "TAD405", "Proyecto de Análisis de Datos", 4, 0, 0, 6, 6, "TAD302", "TAD303", "TAD305");
        addSubject(planId, "TAD406", "Emprendimiento y Gestión de Proyectos", 4, 2, 2, 0, 3);
    }

    // 10452 - Técnico en Ciberseguridad, plan 2021.3
    private void loadCybersecurity() {
        Long planId = createCareerWithPlan("10452", "Técnico en Ciberseguridad",
                "Carrera técnica de dos años orientada a la ciberseguridad", "2021.3");
        // Semester 1
        addSubject(planId, "TCS101", "Fundamentos de Programación", 1, 2, 2, 2, 6);
        addSubject(planId, "TCS102", "Fundamentos de Sistemas Computacionales", 1, 2, 0, 2, 5);
        addSubject(planId, "TCS103", "Redes de Computadores I", 1, 2, 0, 2, 5);
        addSubject(planId, "TCS104", "Sistemas Operativos", 1, 2, 0, 2, 5);
        addSubject(planId, "TCS105", "Matemática Aplicada", 1, 2, 2, 0, 5);
        addSubject(planId, "TCS106", "Comunicación Efectiva", 1, 2, 2, 0, 4);
        // Semester 2
        addSubject(planId, "TCS201", "Programación para Ciberseguridad", 2, 2, 0, 4, 5, "TCS101");
        addSubject(planId, "TCS202", "Redes de Computadores II", 2, 2, 0, 4, 6, "TCS103");
        addSubject(planId, "TCS203", "Administración de Sistemas Linux", 2, 2, 0, 4, 5, "TCS104");
        addSubject(planId, "TCS204", "Fundamentos de Ciberseguridad", 2, 2, 2, 2, 6, "TCS102", "TCS103");
        addSubject(planId, "TCS205", "Bases de Datos", 2, 2, 0, 2, 5, "TCS101");
        addSubject(planId, "TCS206", "Inglés Técnico", 2, 2, 2, 0, 3);
        // Semester 3
        addSubject(planId, "TCS301", "Seguridad de Redes", 3, 2, 0, 4, 6, "TCS202", "TCS204");
        addSubject(planId, "TCS302", "Seguridad de Sistemas Operativos", 3, 2, 0, 4, 5, "TCS203", "TCS204");
        addSubject(planId, "TCS303", "Criptografía Aplicada", 3, 2, 2, 2, 5, "TCS204");
        addSubject(planId, "TCS304", "Seguridad de Aplicaciones Web", 3, 2, 0, 4, 5, "TCS201", "TCS204");
        addSubject(planId, "TCS305", "Monitoreo y Detección de Amenazas", 3, 2, 0, 4, 5, "TCS202", "TCS204");
        addSubject(planId, "TCS306", "Legislación y Ética en Ciberseguridad", 3, 2, 2, 0, 4, "TCS204");
        // Semester 4
        addSubject(planId, "TCS401", "Ethical Hacking y Pruebas de Penetración", 4, 2, 0, 4, 6, "TCS301", "TCS302");
        addSubject(planId, "TCS402", "Gestión de Incidentes de Ciberseguridad", 4, 2, 0, 4, 5, "TCS305");
        addSubject(planId, "TCS403", "Seguridad en Cloud", 4, 2, 0, 4, 5, "TCS301", "TCS302");
        addSubject(planId, "TCS404", "Análisis Forense Digital", 4, 2, 0, 4, 5, "TCS302");
        addSubject(planId, "TCS405", "Proyecto de Ciberseguridad", 4, 0, 0, 6, 6, "TCS301", "TCS302", "TCS305");
        addSubject(planId, "TCS406", "Gestión de Riesgos de Ciberseguridad", 4, 2, 2, 0, 3, "TCS204");
    }

    // Creates the career and its CURRENT study plan; returns the plan id
    private Long createCareerWithPlan(String code, String name, String description, String planIdentifier) {
        Career career = careerService.createCareer(new CareerRequest(code, name, description));
        StudyPlan plan = studyPlanService.createStudyPlan(career.getId(), new StudyPlanRequest(planIdentifier));
        return plan.getId();
    }

    // Goes through SubjectService, so every RN-3.x rule also checks the appendix data
    private void addSubject(Long planId, String code, String name, int semester,
                            int theory, int exercise, int lab, int sct, String... prerequisiteCodes) {
        SubjectRequest request = new SubjectRequest(code, name, semester, theory, exercise, lab, sct,
                List.of(prerequisiteCodes));
        subjectService.createSubject(planId, request);
    }
}