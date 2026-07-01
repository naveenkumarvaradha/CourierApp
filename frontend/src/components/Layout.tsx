import { useState } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import {
  AppBar,
  Avatar,
  Box,
  Button,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  Stack,
  Toolbar,
  Tooltip,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import DashboardIcon from '@mui/icons-material/Dashboard';
import PeopleIcon from '@mui/icons-material/People';
import SecurityIcon from '@mui/icons-material/Security';
import RuleIcon from '@mui/icons-material/Rule';
import BusinessIcon from '@mui/icons-material/Business';
import AltRouteIcon from '@mui/icons-material/AltRoute';
import InventoryIcon from '@mui/icons-material/Inventory';
import TuneIcon from '@mui/icons-material/Tune';
import DomainIcon from '@mui/icons-material/Domain';
import HistoryIcon from '@mui/icons-material/History';
import ContactsIcon from '@mui/icons-material/Contacts';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import AssessmentIcon from '@mui/icons-material/Assessment';
import LogoutIcon from '@mui/icons-material/Logout';
import LockIcon from '@mui/icons-material/Lock';
import ApartmentIcon from '@mui/icons-material/Apartment';
import { useAuth } from '../context/AuthContext';
import ChangePasswordDialog from './ChangePasswordDialog';

interface NavItem {
  label: string;
  to: string;
  icon: JSX.Element;
  permissions: string[];
  group: string;
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard',        to: '/',                         icon: <DashboardIcon />,      permissions: [],               group: 'General' },
  { label: 'Users',            to: '/admin/users',              icon: <PeopleIcon />,         permissions: ['ADMIN_VIEW'],   group: 'Admin' },
  { label: 'Roles',            to: '/admin/roles',              icon: <SecurityIcon />,       permissions: ['ADMIN_VIEW'],   group: 'Admin' },
  { label: 'Approval Routing', to: '/admin/approval-routing',   icon: <RuleIcon />,           permissions: ['ADMIN_VIEW'],   group: 'Admin' },
  { label: 'Company Setup',    to: '/admin/company-settings',   icon: <BusinessIcon />,       permissions: ['ADMIN_VIEW'],   group: 'Admin' },
  { label: 'Departments',      to: '/admin/departments',        icon: <DomainIcon />,         permissions: ['ADMIN_VIEW'],   group: 'Admin' },
  { label: 'Courier Ways',     to: '/admin/courier-ways',       icon: <AltRouteIcon />,       permissions: ['ADMIN_VIEW'],   group: 'Admin' },
  { label: 'Package Types',    to: '/admin/package-types',      icon: <InventoryIcon />,      permissions: ['ADMIN_VIEW'],   group: 'Admin' },
  { label: 'Flex Fields',      to: '/admin/flex-fields',        icon: <TuneIcon />,           permissions: ['ADMIN_VIEW'],   group: 'Admin' },
  { label: 'Audit Logs',      to: '/admin/audit-logs',         icon: <HistoryIcon />,        permissions: ['ADMIN_VIEW'],   group: 'Admin' },
  { label: 'Companies',       to: '/admin/companies',          icon: <ApartmentIcon />,      permissions: ['ADMIN_VIEW'],   group: 'Admin' },
  { label: 'Parties',          to: '/master/parties',           icon: <ContactsIcon />,       permissions: ['MASTER_VIEW'],  group: 'Master' },
  { label: 'Bookings',         to: '/bookings',                 icon: <LocalShippingIcon />,  permissions: ['BOOKING_VIEW'], group: 'Booking' },
  { label: 'Reports',          to: '/reports',                  icon: <AssessmentIcon />,     permissions: ['REPORTS_VIEW'], group: 'Reports' },
];

export default function Layout() {
  const { user, logout, hasAnyPermission } = useAuth();
  const location = useLocation();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  // mobile drawer
  const [drawerOpen, setDrawerOpen] = useState(false);
  // user avatar menu
  const [userAnchor, setUserAnchor] = useState<null | HTMLElement>(null);
  const [changePwOpen, setChangePwOpen] = useState(false);
  // per-group dropdown menus (desktop)
  const [groupAnchors, setGroupAnchors] = useState<Record<string, HTMLElement | null>>({});

  const visibleItems = NAV_ITEMS.filter(
    (item) => item.permissions.length === 0 || hasAnyPermission(item.permissions),
  );

  const groups = Array.from(new Set(visibleItems.map((i) => i.group)));

  const isGroupActive = (group: string) =>
    visibleItems
      .filter((i) => i.group === group)
      .some((i) => (i.to === '/' ? location.pathname === '/' : location.pathname.startsWith(i.to)));

  const openGroup = (group: string, el: HTMLElement) =>
    setGroupAnchors((prev) => ({ ...prev, [group]: el }));
  const closeGroup = (group: string) =>
    setGroupAnchors((prev) => ({ ...prev, [group]: null }));

  /* ── Mobile drawer ─────────────────────────────────────────── */
  const mobileDrawer = (
    <Box sx={{ width: 260 }}>
      <Toolbar>
        <LocalShippingIcon sx={{ mr: 1, color: 'primary.main' }} />
        <Typography variant="h6" fontWeight={700}>CourierApp</Typography>
      </Toolbar>
      <Divider />
      <List dense>
        {groups.map((group) => (
          <Box key={group}>
            <Typography variant="overline" sx={{ px: 2, color: 'text.secondary', display: 'block', mt: 1 }}>
              {group}
            </Typography>
            {visibleItems.filter((i) => i.group === group).map((item) => {
              const selected = item.to === '/' ? location.pathname === '/' : location.pathname.startsWith(item.to);
              return (
                <ListItemButton key={item.to} component={Link} to={item.to} selected={selected}
                  onClick={() => setDrawerOpen(false)}>
                  <ListItemIcon sx={{ minWidth: 36 }}>{item.icon}</ListItemIcon>
                  <ListItemText primary={item.label} />
                </ListItemButton>
              );
            })}
          </Box>
        ))}
      </List>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      {/* ── Top AppBar ─────────────────────────────────────────── */}
      <AppBar position="sticky" elevation={1} sx={{ zIndex: theme.zIndex.drawer + 1 }}>
        <Toolbar sx={{ gap: 0.5 }}>
          {/* Mobile hamburger */}
          {isMobile && (
            <IconButton color="inherit" edge="start" onClick={() => setDrawerOpen(true)} sx={{ mr: 1 }}>
              <MenuIcon />
            </IconButton>
          )}

          {/* Logo / brand */}
          <LocalShippingIcon sx={{ mr: 0.5 }} />
          <Typography variant="h6" fontWeight={700} noWrap sx={{ mr: 2 }}>
            Courier Booking
          </Typography>

          {/* Desktop nav groups */}
          {!isMobile && (
            <Stack direction="row" spacing={0.5} flex={1}>
              {groups.map((group) => {
                const items = visibleItems.filter((i) => i.group === group);
                if (items.length === 1 && items[0].group === 'General') {
                  // Dashboard — single link button
                  return (
                    <Button key={group} color="inherit" component={Link} to={items[0].to}
                      sx={{ fontWeight: isGroupActive(group) ? 700 : 400, textTransform: 'none' }}>
                      {items[0].label}
                    </Button>
                  );
                }
                if (items.length === 1) {
                  return (
                    <Button key={group} color="inherit" component={Link} to={items[0].to}
                      sx={{ fontWeight: isGroupActive(group) ? 700 : 400, textTransform: 'none' }}>
                      {group}
                    </Button>
                  );
                }
                // Multi-item group → dropdown
                return (
                  <Box key={group}>
                    <Button
                      color="inherit"
                      endIcon={<KeyboardArrowDownIcon />}
                      onClick={(e) => openGroup(group, e.currentTarget)}
                      sx={{ fontWeight: isGroupActive(group) ? 700 : 400, textTransform: 'none' }}
                    >
                      {group}
                    </Button>
                    <Menu
                      anchorEl={groupAnchors[group]}
                      open={Boolean(groupAnchors[group])}
                      onClose={() => closeGroup(group)}
                      anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
                    >
                      {items.map((item) => {
                        const selected = item.to === '/' ? location.pathname === '/' : location.pathname.startsWith(item.to);
                        return (
                          <MenuItem key={item.to} component={Link} to={item.to}
                            selected={selected} onClick={() => closeGroup(group)}
                            sx={{ gap: 1 }}>
                            {item.icon}
                            {item.label}
                          </MenuItem>
                        );
                      })}
                    </Menu>
                  </Box>
                );
              })}
            </Stack>
          )}
          {isMobile && <Box flex={1} />}

          {/* User avatar */}
          <Tooltip title={user?.fullName ?? ''}>
            <IconButton color="inherit" onClick={(e) => setUserAnchor(e.currentTarget)} size="small">
              <Avatar sx={{ width: 32, height: 32, bgcolor: 'secondary.main', fontSize: 14 }}>
                {user?.fullName?.charAt(0).toUpperCase()}
              </Avatar>
            </IconButton>
          </Tooltip>
          <Menu anchorEl={userAnchor} open={Boolean(userAnchor)} onClose={() => setUserAnchor(null)}>
            <MenuItem disabled sx={{ flexDirection: 'column', alignItems: 'flex-start' }}>
              <Typography variant="body2" fontWeight={600}>{user?.fullName}</Typography>
              <Typography variant="caption" color="text.secondary">{user?.companyName ?? ''}</Typography>
              <Typography variant="caption" color="text.secondary">{user?.roles.join(', ')}</Typography>
            </MenuItem>
            <Divider />
            <MenuItem onClick={() => { setUserAnchor(null); setChangePwOpen(true); }}>
              <ListItemIcon><LockIcon fontSize="small" /></ListItemIcon>
              Change Password
            </MenuItem>
            <MenuItem onClick={logout}>
              <ListItemIcon><LogoutIcon fontSize="small" /></ListItemIcon>
              Logout
            </MenuItem>
          </Menu>
          <ChangePasswordDialog open={changePwOpen} onClose={() => setChangePwOpen(false)} />
        </Toolbar>
      </AppBar>

      {/* Mobile drawer */}
      <Drawer open={drawerOpen} onClose={() => setDrawerOpen(false)} variant="temporary">
        {mobileDrawer}
      </Drawer>

      {/* ── Page content ─────────────────────────────────────── */}
      <Box component="main" sx={{ flexGrow: 1, p: { xs: 2, md: 3 }, bgcolor: 'grey.50' }}>
        <Outlet />
      </Box>
    </Box>
  );
}
