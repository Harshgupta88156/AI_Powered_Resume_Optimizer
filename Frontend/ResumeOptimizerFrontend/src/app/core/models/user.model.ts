import { UserRole, AuthProvider } from './auth.model';

export interface UserResponse {
  userId: number;
  name: string;
  email: string;
  role: UserRole;
  provider: AuthProvider;
  providerId?: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UserUpdateRequest {
  name: string;
}

// ── Extended profile ───────────────────────────────────────────────────────

export type EmploymentType =
  | 'FULL_TIME'
  | 'PART_TIME'
  | 'INTERNSHIP'
  | 'CONTRACT'
  | 'FREELANCE'
  | 'SELF_EMPLOYED';

export type Proficiency = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'EXPERT';

export interface Experience {
  experienceId?: number | null;
  jobTitle: string;
  company: string;
  location?: string | null;
  employmentType?: EmploymentType | string | null;
  /** ISO date, yyyy-MM-dd. */
  startDate?: string | null;
  endDate?: string | null;
  currentRole?: boolean | null;
  description?: string | null;
}

export interface Education {
  educationId?: number | null;
  /** College / university / school. */
  institution: string;
  degree?: string | null;
  fieldOfStudy?: string | null;
  location?: string | null;
  startYear?: number | null;
  endYear?: number | null;
  /** Free text: "8.4 CGPA" or "First Class". */
  grade?: string | null;
  description?: string | null;
}

export interface Certification {
  certificationId?: number | null;
  name: string;
  issuingOrganization?: string | null;
  credentialId?: string | null;
  credentialUrl?: string | null;
  issueDate?: string | null;
  expiryDate?: string | null;
}

export interface Skill {
  name: string;
  proficiency?: Proficiency | string | null;
}

export interface UserProfileResponse {
  userId: number;
  name: string;
  email: string;
  role: UserRole;
  provider: AuthProvider;
  active: boolean;
  createdAt: string;

  phone?: string | null;
  location?: string | null;
  city?: string | null;
  country?: string | null;
  openToRelocation?: boolean | null;

  headline?: string | null;
  summary?: string | null;
  currentTitle?: string | null;
  currentCompany?: string | null;
  totalExperienceYears?: number | null;

  linkedinUrl?: string | null;
  githubUrl?: string | null;
  portfolioUrl?: string | null;
  leetcodeUrl?: string | null;
  twitterUrl?: string | null;

  experiences: Experience[];
  educations: Education[];
  certifications: Certification[];
  skills: Skill[];

  profileUpdatedAt?: string | null;
  completionPercentage: number;
}

/**
 * Omitting a collection leaves it untouched server-side; sending `[]` clears it.
 * That distinction lets the page save one section without wiping the others.
 */
export interface UserProfileUpdateRequest {
  name?: string;
  phone?: string | null;
  location?: string | null;
  city?: string | null;
  country?: string | null;
  openToRelocation?: boolean | null;

  headline?: string | null;
  summary?: string | null;
  currentTitle?: string | null;
  currentCompany?: string | null;
  totalExperienceYears?: number | null;

  linkedinUrl?: string | null;
  githubUrl?: string | null;
  portfolioUrl?: string | null;
  leetcodeUrl?: string | null;
  twitterUrl?: string | null;

  experiences?: Experience[];
  educations?: Education[];
  certifications?: Certification[];
  skills?: Skill[];
}
