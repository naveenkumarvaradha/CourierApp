export interface Company {
  id: number;
  companyCode: string;
  name: string;
  active: boolean;
}

export interface AuditLog {
  id: number;
  module: string;
  action: string;
  entityId: number | null;
  entityName: string | null;
  performedBy: string;
  details: string | null;
  createdAt: string;
}

export interface TokenResponse {
  accessToken: string | null;
  refreshToken: string | null;
  tokenType: string | null;
  expiresIn: number;
}

export interface CurrentUser {
  id: number;
  username: string;
  fullName: string;
  email: string;
  companyId: number | null;
  companyCode: string | null;
  companyName: string | null;
  roles: string[];
  permissions: string[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface Permission {
  id: number;
  module: string;
  action: string;
  code: string;
  description: string;
}

export interface RoleSummary {
  id: number;
  name: string;
}

export interface Role {
  id: number;
  name: string;
  description: string;
  systemRole: boolean;
  permissions: Permission[];
}

export interface UserAccount {
  id: number;
  username: string;
  fullName: string;
  email: string;
  phone: string | null;
  active: boolean;
  departmentId: number | null;
  departmentName: string | null;
  companyId: number | null;
  companyCode: string | null;
  companyName: string | null;
  roles: RoleSummary[];
  directPermissions: Permission[];
  inactiveAt: string | null;
  createdAt: string;
  createdBy: string;
  updatedAt: string;
  updatedBy: string;
}

export interface ApprovalRouting {
  id: number;
  roleId: number | null;
  roleName: string | null;
  userId: number | null;
  username: string | null;
  creatorRoleId: number | null;
  creatorRoleName: string | null;
  creatorUserId: number | null;
  creatorUsername: string | null;
  active: boolean;
  module: string;
}

export type PartyType = 'SENDER' | 'RECEIVER' | 'BOTH';
export type PartyStatus = 'PENDING_APPROVAL' | 'ACTIVE' | 'INACTIVE' | 'REJECTED';

export interface Party {
  id: number;
  partyCode: string;
  partyName: string;
  addressLine1: string;
  addressLine2: string | null;
  city: string;
  state: string;
  pincode: string;
  country: string;
  phone: string | null;
  email: string | null;
  gstin: string | null;
  partyType: PartyType;
  active: boolean;
  partyStatus: PartyStatus;
  companyName: string | null;
  currentApprovalLevel: number;
  createdBy: string | null;
  createdAt: string | null;
  pendingApprovers?: string[] | null;
}

export interface MailConfig {
  smtpHost: string | null;
  smtpPort: number | null;
  smtpUsername: string | null;
  smtpFromName: string | null;
  smtpTls: boolean | null;
  configured: boolean;
}

export interface CompanySettings {
  id: number;
  companyName: string;
  addressLine1: string;
  addressLine2: string | null;
  city: string;
  state: string;
  pincode: string;
  country: string;
  phone: string | null;
  email: string | null;
  gstin: string | null;
  // SMTP config (password never returned)
  smtpHost: string | null;
  smtpPort: number | null;
  smtpUsername: string | null;
  smtpFromName: string | null;
  smtpTls: boolean | null;
  smtpConfigured: boolean;
}

export interface PasswordPolicy {
  id: number | null;
  restrictLastPasswords: number;
  passwordExpiryDays: number;
  expiryReminderDays: number;
  sessionTimeoutHours: number;
  sessionTimeoutMinutes: number;
  maxLoginAttempts: number;
  minPasswordLength: number;
  requireUppercase: boolean;
  requireLowercase: boolean;
  requireDigit: boolean;
  requireSpecialChar: boolean;
}

export interface ReportSchedule {
  id: number;
  scheduleName: string;
  reportType: string;
  frequency: string;
  dayOfWeek: number | null;
  dayOfMonth: number | null;
  monthOfYear: number | null;
  recipientEmails: string;
  fileFormat: string;
  enabled: boolean;
  lastRunAt: string | null;
  nextRunAt: string | null;
  createdBy: string | null;
  createdAt: string | null;
}

export interface CourierWay {
  id: number;
  name: string;
  active: boolean;
}

export interface Department {
  id: number;
  name: string;
  active: boolean;
}

export interface PackageType {
  id: number;
  name: string;
  active: boolean;
}

export type FlexFieldType = 'TEXT' | 'DROPDOWN_SINGLE' | 'DROPDOWN_MULTI';

export interface FlexFieldOption {
  id: number;
  optionValue: string;
  sortOrder: number;
  active: boolean;
}

export interface FlexFieldDefinition {
  id: number;
  module: string;
  fieldName: string;
  fieldLabel: string;
  fieldType: FlexFieldType;
  required: boolean;
  active: boolean;
  sortOrder: number;
  options: FlexFieldOption[];
}

/** fieldId → raw value */
export type FlexFieldValues = Record<number, string>;

export type CourierMode = 'AIR' | 'SURFACE' | 'EXPRESS';
export type PaymentMode = 'PREPAID' | 'TOPAY';
export type BookingStatus =
  | 'BOOKED'
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'IN_TRANSIT'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'REJECTED'
  | 'PENDING_CANCELLATION';

export interface Booking {
  id: number;
  bookingNumber: string;
  bookingDate: string;
  sender: Party;
  receiver: Party;
  courierWay: CourierWay | null;
  packageType: PackageType | null;
  itemDescription: string;
  weightKg: number;
  noOfPackages: number;
  courierMode: CourierMode;
  specialInstructions: string | null;
  status: BookingStatus;
  awbNumber: string | null;
  approverUsername: string | null;
  approvalTimestamp: string | null;
  approvalRemarks: string | null;
  companyPoNo: string | null;
  printTaken: boolean;
  cancellationRemarks: string | null;
  currentApprovalLevel: number;
  createdAt: string | null;
  createdBy: string | null;
  pendingApprovers?: string[] | null;
}

export interface PartyBreakdown {
  partyCode: string;
  partyName: string;
  bookingCount: number;
  totalCharges: number;
}

export interface DashboardTasks {
  bookingsPendingMyApproval: Booking[];
  myBookingsPendingSent: Booking[];
  partiesPendingMyApproval: Party[];
  myPartiesPendingSent: Party[];
  pendingToPrint: Booking[];
  allPendingApprovalBookings: Booking[];
  allPendingApprovalParties: Party[];
}

export interface ApprovalInfo {
  currentLevel: number;
  maxLevel: number;
  approvers: string[];
  summary: string;
}

export interface ApprovalRouting {
  id: number;
  roleId: number | null;
  roleName: string | null;
  userId: number | null;
  username: string | null;
  creatorRoleId: number | null;
  creatorRoleName: string | null;
  creatorUserId: number | null;
  creatorUsername: string | null;
  active: boolean;
  module: string;
  level: number;
}

export interface ReportSummary {
  fromDate: string;
  toDate: string;
  granularity: string;
  totalBookings: number;
  totalCharges: number;
  totalFreight: number;
  totalDeclaredValue: number;
  countByStatus: Record<string, number>;
  countByMode: Record<string, number>;
  chargesByMode: Record<string, number>;
  bySender: PartyBreakdown[];
  byReceiver: PartyBreakdown[];
  bookings: Booking[];
}


export interface StickerField {
  fieldKey: string;
  label: string;
  visible: boolean;
  sortOrder: number;
  section: string; // HEADER | FROM | TO | DETAILS | BOTTOM
}
