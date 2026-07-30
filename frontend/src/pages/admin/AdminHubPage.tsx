import { Link } from 'react-router-dom';
import { Card, CardActionArea, CardContent, Grid, Stack, Typography } from '@mui/material';
import PeopleIcon from '@mui/icons-material/People';
import SecurityIcon from '@mui/icons-material/Security';
import RuleIcon from '@mui/icons-material/Rule';
import AltRouteIcon from '@mui/icons-material/AltRoute';
import InventoryIcon from '@mui/icons-material/Inventory';
import TuneIcon from '@mui/icons-material/Tune';
import DomainIcon from '@mui/icons-material/Domain';
import HistoryIcon from '@mui/icons-material/History';
import LockIcon from '@mui/icons-material/Lock';
import ApartmentIcon from '@mui/icons-material/Apartment';
import StoreIcon from '@mui/icons-material/Store';
import StickyNote2Icon from '@mui/icons-material/StickyNote2';
import EmailIcon from '@mui/icons-material/Email';
import MonitorHeartIcon from '@mui/icons-material/MonitorHeart';
import EventRepeatIcon from '@mui/icons-material/EventRepeat';
import { useAuth } from '../../context/AuthContext';

interface AdminTile {
  label: string;
  to: string;
  icon: JSX.Element;
  description: string;
  permissions: string[];
}

const TILES: AdminTile[] = [
  { label: 'Users', to: '/admin/users', icon: <PeopleIcon fontSize="large" />, description: 'Manage user accounts and role assignments', permissions: ['ADMIN_VIEW'] },
  { label: 'Roles', to: '/admin/roles', icon: <SecurityIcon fontSize="large" />, description: 'Define roles and their permissions', permissions: ['ADMIN_VIEW'] },
  { label: 'Approval Routing', to: '/admin/approval-routing', icon: <RuleIcon fontSize="large" />, description: 'Configure who approves Bookings, Parties and DCs', permissions: ['ADMIN_VIEW'] },
  { label: 'Departments', to: '/admin/departments', icon: <DomainIcon fontSize="large" />, description: 'Manage organizational departments', permissions: ['ADMIN_VIEW'] },
  { label: 'Courier Ways', to: '/admin/courier-ways', icon: <AltRouteIcon fontSize="large" />, description: 'Manage courier/transport mode options', permissions: ['ADMIN_VIEW'] },
  { label: 'Package Types', to: '/admin/package-types', icon: <InventoryIcon fontSize="large" />, description: 'Manage package type options', permissions: ['ADMIN_VIEW'] },
  { label: 'Units', to: '/admin/units', icon: <StoreIcon fontSize="large" />, description: 'Manage company branch/unit addresses', permissions: ['ADMIN_VIEW'] },
  { label: 'Flex Fields', to: '/admin/flex-fields', icon: <TuneIcon fontSize="large" />, description: 'Configure custom fields for bookings and parties', permissions: ['ADMIN_VIEW'] },
  { label: 'Audit Logs', to: '/admin/audit-logs', icon: <HistoryIcon fontSize="large" />, description: 'Review a history of system actions', permissions: ['ADMIN_VIEW'] },
  { label: 'Companies', to: '/admin/companies', icon: <ApartmentIcon fontSize="large" />, description: 'Manage companies and their settings', permissions: ['ADMIN_VIEW'] },
  { label: 'Report Schedules', to: '/admin/report-schedules', icon: <EventRepeatIcon fontSize="large" />, description: 'Schedule recurring report emails', permissions: ['ADMIN_VIEW'] },
  { label: 'Password Policy', to: '/admin/password-policy', icon: <LockIcon fontSize="large" />, description: 'Configure password strength requirements', permissions: ['ADMIN_VIEW'] },
  { label: 'Sticker Config', to: '/admin/sticker-config', icon: <StickyNote2Icon fontSize="large" />, description: 'Configure fields shown on shipping labels', permissions: ['ADMIN_VIEW'] },
  { label: 'Mail Config', to: '/admin/mail-config', icon: <EmailIcon fontSize="large" />, description: 'Configure outgoing email settings', permissions: ['ADMIN_VIEW'] },
  { label: 'System Status', to: '/admin/system-status', icon: <MonitorHeartIcon fontSize="large" />, description: 'View system health and diagnostics', permissions: ['ADMIN_VIEW'] },
];

export default function AdminHubPage() {
  const { hasPermission } = useAuth();
  const tiles = TILES.filter((t) => t.permissions.every((p) => hasPermission(p)));

  return (
    <Stack spacing={2}>
      <Typography variant="h5" fontWeight={600}>Admin</Typography>
      <Typography variant="body2" color="text.secondary">
        Manage system configuration, master data and organization settings.
      </Typography>
      <Grid container spacing={2}>
        {tiles.map((tile) => (
          <Grid item xs={12} sm={6} md={4} lg={3} key={tile.to}>
            <Card variant="outlined" sx={{ height: '100%' }}>
              <CardActionArea component={Link} to={tile.to} sx={{ height: '100%', p: 1 }}>
                <CardContent>
                  <Stack spacing={1.5} alignItems="flex-start">
                    <Stack sx={{ color: 'primary.main' }}>{tile.icon}</Stack>
                    <Typography variant="subtitle1" fontWeight={600}>{tile.label}</Typography>
                    <Typography variant="body2" color="text.secondary">{tile.description}</Typography>
                  </Stack>
                </CardContent>
              </CardActionArea>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Stack>
  );
}
