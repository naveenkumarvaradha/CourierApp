export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface CurrentUser {
  id: number;
  username: string;
  fullName: string;
  email: string;
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
  roles: RoleSummary[];
  directPermissions: Permission[];
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
  active: boolean;
}

export type PartyType = 'SENDER' | 'RECEIVER' | 'BOTH';

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
}

export type CourierMode = 'AIR' | 'SURFACE' | 'EXPRESS';
export type PaymentMode = 'PREPAID' | 'TOPAY';
export type BookingStatus =
  | 'BOOKED'
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'IN_TRANSIT'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'REJECTED';

export interface Booking {
  id: number;
  bookingNumber: string;
  bookingDate: string;
  sender: Party;
  receiver: Party;
  itemDescription: string;
  weightKg: number;
  noOfPackages: number;
  courierMode: CourierMode;
  declaredValue: number | null;
  freightCharges: number;
  totalCharges: number;
  paymentMode: PaymentMode;
  specialInstructions: string | null;
  status: BookingStatus;
  approverUsername: string | null;
  approvalTimestamp: string | null;
  approvalRemarks: string | null;
}

export interface PartyBreakdown {
  partyCode: string;
  partyName: string;
  bookingCount: number;
  totalCharges: number;
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
