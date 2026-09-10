import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { extractErrorMessage } from '../../../core/utils/error.util';
import {
  Certification,
  Education,
  EmploymentType,
  Experience,
  Proficiency,
  UserProfileResponse,
  UserProfileUpdateRequest
} from '../../../core/models/user.model';
import { Spinner } from '../../../shared/components/spinner/spinner';

/** Blank string -> null, so the backend stores "unset" rather than "". */
function nullIfBlank(value: unknown): string | null {
  if (typeof value !== 'string') return (value as string) ?? null;
  const trimmed = value.trim();
  return trimmed.length ? trimmed : null;
}

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, Spinner],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.css'
})
export class ProfilePage implements OnInit {

  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private authService = inject(AuthService);
  private toast = inject(ToastService);

  loading = signal(true);
  saving = signal(false);
  error = signal('');
  profile = signal<UserProfileResponse | null>(null);

  completion = computed(() => this.profile()?.completionPercentage ?? 0);

  employmentTypes: EmploymentType[] = [
    'FULL_TIME', 'PART_TIME', 'INTERNSHIP', 'CONTRACT', 'FREELANCE', 'SELF_EMPLOYED'
  ];

  proficiencies: Proficiency[] = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT'];

  /** Free-text box the user types comma-separated skills into. */
  skillsInput = signal('');

  form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    headline: ['', Validators.maxLength(160)],
    summary: ['', Validators.maxLength(4000)],

    phone: ['', Validators.pattern(/^$|^[+()\-\s0-9]{6,32}$/)],
    location: ['', Validators.maxLength(160)],
    city: ['', Validators.maxLength(100)],
    country: ['', Validators.maxLength(100)],
    openToRelocation: [false],

    currentTitle: ['', Validators.maxLength(160)],
    currentCompany: ['', Validators.maxLength(160)],
    totalExperienceYears: [null as number | null, [Validators.min(0), Validators.max(60)]],

    linkedinUrl: ['', Validators.maxLength(300)],
    githubUrl: ['', Validators.maxLength(300)],
    portfolioUrl: ['', Validators.maxLength(300)],
    leetcodeUrl: ['', Validators.maxLength(300)],
    twitterUrl: ['', Validators.maxLength(300)],

    experiences: this.fb.array([] as FormGroup[]),
    educations: this.fb.array([] as FormGroup[]),
    certifications: this.fb.array([] as FormGroup[])
  });

  get experiences(): FormArray { return this.form.get('experiences') as FormArray; }
  get educations(): FormArray { return this.form.get('educations') as FormArray; }
  get certifications(): FormArray { return this.form.get('certifications') as FormArray; }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');

    this.userService.getMyProfile().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.patchForm(profile);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(extractErrorMessage(err, 'Could not load your profile.'));
        this.loading.set(false);
      }
    });
  }

  private patchForm(profile: UserProfileResponse): void {
    this.form.patchValue({
      name: profile.name ?? '',
      headline: profile.headline ?? '',
      summary: profile.summary ?? '',
      phone: profile.phone ?? '',
      location: profile.location ?? '',
      city: profile.city ?? '',
      country: profile.country ?? '',
      openToRelocation: profile.openToRelocation ?? false,
      currentTitle: profile.currentTitle ?? '',
      currentCompany: profile.currentCompany ?? '',
      totalExperienceYears: profile.totalExperienceYears ?? null,
      linkedinUrl: profile.linkedinUrl ?? '',
      githubUrl: profile.githubUrl ?? '',
      portfolioUrl: profile.portfolioUrl ?? '',
      leetcodeUrl: profile.leetcodeUrl ?? '',
      twitterUrl: profile.twitterUrl ?? ''
    });

    this.experiences.clear();
    (profile.experiences ?? []).forEach(e => this.experiences.push(this.experienceGroup(e)));

    this.educations.clear();
    (profile.educations ?? []).forEach(e => this.educations.push(this.educationGroup(e)));

    this.certifications.clear();
    (profile.certifications ?? []).forEach(c => this.certifications.push(this.certificationGroup(c)));

    this.skillsInput.set((profile.skills ?? []).map(s => s.name).join(', '));

    this.form.markAsPristine();
  }

  // ── Row factories ───────────────────────────────────────────────────────

  private experienceGroup(value: Partial<Experience> = {}): FormGroup {
    const group = this.fb.group({
      experienceId: [value.experienceId ?? null],
      jobTitle: [value.jobTitle ?? '', [Validators.required, Validators.maxLength(160)]],
      company: [value.company ?? '', [Validators.required, Validators.maxLength(160)]],
      location: [value.location ?? ''],
      employmentType: [value.employmentType ?? 'FULL_TIME'],
      startDate: [value.startDate ?? null],
      endDate: [{ value: value.endDate ?? null, disabled: !!value.currentRole }],
      currentRole: [!!value.currentRole],
      description: [value.description ?? '', Validators.maxLength(4000)]
    });

    // "I currently work here" and an end date are mutually exclusive; disable
    // the field rather than letting the user save a contradiction.
    group.get('currentRole')!.valueChanges.subscribe(isCurrent => {
      const endDate = group.get('endDate')!;
      if (isCurrent) {
        endDate.setValue(null);
        endDate.disable();
      } else {
        endDate.enable();
      }
    });

    return group;
  }

  private educationGroup(value: Partial<Education> = {}): FormGroup {
    return this.fb.group({
      educationId: [value.educationId ?? null],
      institution: [value.institution ?? '', [Validators.required, Validators.maxLength(200)]],
      degree: [value.degree ?? ''],
      fieldOfStudy: [value.fieldOfStudy ?? ''],
      location: [value.location ?? ''],
      startYear: [value.startYear ?? null, [Validators.min(1900), Validators.max(2100)]],
      endYear: [value.endYear ?? null, [Validators.min(1900), Validators.max(2100)]],
      grade: [value.grade ?? ''],
      description: [value.description ?? '']
    });
  }

  private certificationGroup(value: Partial<Certification> = {}): FormGroup {
    return this.fb.group({
      certificationId: [value.certificationId ?? null],
      name: [value.name ?? '', [Validators.required, Validators.maxLength(200)]],
      issuingOrganization: [value.issuingOrganization ?? ''],
      credentialId: [value.credentialId ?? ''],
      credentialUrl: [value.credentialUrl ?? ''],
      issueDate: [value.issueDate ?? null],
      expiryDate: [value.expiryDate ?? null]
    });
  }

  // ── Row add/remove ──────────────────────────────────────────────────────

  addExperience(): void {
    this.experiences.push(this.experienceGroup());
    this.form.markAsDirty();
  }

  removeExperience(index: number): void {
    this.experiences.removeAt(index);
    this.form.markAsDirty();
  }

  addEducation(): void {
    this.educations.push(this.educationGroup());
    this.form.markAsDirty();
  }

  removeEducation(index: number): void {
    this.educations.removeAt(index);
    this.form.markAsDirty();
  }

  addCertification(): void {
    this.certifications.push(this.certificationGroup());
    this.form.markAsDirty();
  }

  removeCertification(index: number): void {
    this.certifications.removeAt(index);
    this.form.markAsDirty();
  }

  onSkillsInput(value: string): void {
    this.skillsInput.set(value);
    this.form.markAsDirty();
  }

  get parsedSkills(): string[] {
    const seen = new Set<string>();
    return this.skillsInput()
      .split(',')
      .map(s => s.trim())
      .filter(Boolean)
      .filter(s => {
        const key = s.toLowerCase();
        if (seen.has(key)) return false;
        seen.add(key);
        return true;
      })
      .slice(0, 100);
  }

  // ── Save ────────────────────────────────────────────────────────────────

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toast.error('Some fields need attention. Check the highlighted rows.');
      return;
    }

    this.saving.set(true);

    // getRawValue() rather than value: disabled controls (a current role's end
    // date) are otherwise dropped from the payload entirely.
    const raw = this.form.getRawValue() as Record<string, any>;

    const payload: UserProfileUpdateRequest = {
      name: (raw['name'] ?? '').trim(),
      headline: nullIfBlank(raw['headline']),
      summary: nullIfBlank(raw['summary']),
      phone: nullIfBlank(raw['phone']),
      location: nullIfBlank(raw['location']),
      city: nullIfBlank(raw['city']),
      country: nullIfBlank(raw['country']),
      openToRelocation: !!raw['openToRelocation'],
      currentTitle: nullIfBlank(raw['currentTitle']),
      currentCompany: nullIfBlank(raw['currentCompany']),
      totalExperienceYears: raw['totalExperienceYears'] != null && raw['totalExperienceYears'] !== ''
        ? Number(raw['totalExperienceYears'])
        : null,
      linkedinUrl: nullIfBlank(raw['linkedinUrl']),
      githubUrl: nullIfBlank(raw['githubUrl']),
      portfolioUrl: nullIfBlank(raw['portfolioUrl']),
      leetcodeUrl: nullIfBlank(raw['leetcodeUrl']),
      twitterUrl: nullIfBlank(raw['twitterUrl']),

      experiences: (raw['experiences'] ?? []).map((e: any) => ({
        experienceId: e.experienceId ?? null,
        jobTitle: (e.jobTitle ?? '').trim(),
        company: (e.company ?? '').trim(),
        location: nullIfBlank(e.location),
        employmentType: nullIfBlank(e.employmentType),
        startDate: e.startDate || null,
        endDate: e.currentRole ? null : (e.endDate || null),
        currentRole: !!e.currentRole,
        description: nullIfBlank(e.description)
      })),

      educations: (raw['educations'] ?? []).map((e: any) => ({
        educationId: e.educationId ?? null,
        institution: (e.institution ?? '').trim(),
        degree: nullIfBlank(e.degree),
        fieldOfStudy: nullIfBlank(e.fieldOfStudy),
        location: nullIfBlank(e.location),
        startYear: e.startYear ? Number(e.startYear) : null,
        endYear: e.endYear ? Number(e.endYear) : null,
        grade: nullIfBlank(e.grade),
        description: nullIfBlank(e.description)
      })),

      certifications: (raw['certifications'] ?? []).map((c: any) => ({
        certificationId: c.certificationId ?? null,
        name: (c.name ?? '').trim(),
        issuingOrganization: nullIfBlank(c.issuingOrganization),
        credentialId: nullIfBlank(c.credentialId),
        credentialUrl: nullIfBlank(c.credentialUrl),
        issueDate: c.issueDate || null,
        expiryDate: c.expiryDate || null
      })),

      skills: this.parsedSkills.map(name => ({ name }))
    };

    this.userService.updateMyProfile(payload).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.patchForm(profile);
        this.saving.set(false);

        // Header avatar/name reads from the auth signal, not this page.
        this.authService.patchCurrentUser({ name: profile.name });
        this.toast.success('Profile saved.');
      },
      error: (err) => {
        this.saving.set(false);
        this.toast.error(extractErrorMessage(err, 'Could not save your profile.'));
      }
    });
  }

  reset(): void {
    const profile = this.profile();
    if (profile) {
      this.patchForm(profile);
      this.toast.info('Changes discarded.');
    }
  }

  // ── Display helpers ─────────────────────────────────────────────────────

  get initials(): string {
    const name = this.profile()?.name ?? '';
    return name.split(' ').filter(Boolean).slice(0, 2)
      .map(p => p[0]?.toUpperCase()).join('') || 'U';
  }

  get roleLabel(): string {
    return this.profile()?.role === 'ADMIN' ? 'Administrator' : 'User';
  }

  get providerLabel(): string {
    return this.profile()?.provider === 'GITHUB' ? 'GitHub' : 'Email & password';
  }

  labelFor(value: string): string {
    return value.replace(/_/g, ' ').toLowerCase()
      .replace(/\b\w/g, c => c.toUpperCase());
  }
}
