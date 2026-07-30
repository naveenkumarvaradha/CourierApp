import { useState } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import {
  AppBar, Avatar, Box, Button, Chip, Divider, Drawer,
  IconButton, List, ListItemButton, ListItemIcon, ListItemText,
  Menu, MenuItem, Stack, Toolbar, Tooltip, Typography,
  useMediaQuery, useTheme,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import DashboardIcon from '@mui/icons-material/Dashboard';
import SettingsIcon from '@mui/icons-material/Settings';
import LockIcon from '@mui/icons-material/Lock';
import ContactsIcon from '@mui/icons-material/Contacts';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import AssessmentIcon from '@mui/icons-material/Assessment';
import LogoutIcon from '@mui/icons-material/Logout';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import AssignmentReturnIcon from '@mui/icons-material/AssignmentReturn';
import HourglassTopIcon from '@mui/icons-material/HourglassTop';
import PersonIcon from '@mui/icons-material/Person';
import KeyboardArrowRightIcon from '@mui/icons-material/KeyboardArrowRight';
import { useAuth } from '../context/AuthContext';
import ChangePasswordDialog from './ChangePasswordDialog';

interface NavItem {
  label: string;
  to: string;
  icon: JSX.Element;
  permissions: string[];
  group: string;
  badge?: string;
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard',        to: '/',                          icon: <DashboardIcon />,      permissions: [],               group: 'Dashboard' },
  { label: 'Approval Center',  to: '/approval-center',           icon: <HourglassTopIcon />,   permissions: [],               group: 'Approval Center' },
  { label: 'Admin',            to: '/admin',                     icon: <SettingsIcon />,       permissions: ['ADMIN_VIEW'],   group: 'Admin' },
  { label: 'Parties',          to: '/master/parties',            icon: <ContactsIcon />,       permissions: ['MASTER_VIEW'],  group: 'Master' },
  { label: 'Courier Booking',  to: '/bookings',                  icon: <LocalShippingIcon />,  permissions: ['BOOKING_VIEW'], group: 'Courier Booking' },
  { label: 'DC Booking',       to: '/dc',                        icon: <ReceiptLongIcon />,    permissions: ['DELIVERY_CHALLAN_VIEW'], group: 'DC Booking' },
  { label: 'DC Receipt',       to: '/dc-receipts',               icon: <AssignmentReturnIcon />, permissions: ['RECEIPT_VIEW'], group: 'DC Receipt' },
  { label: 'Reports',          to: '/reports',                   icon: <AssessmentIcon />,     permissions: ['REPORTS_VIEW'], group: 'Reports' },
];

const DRAWER_WIDTH = 272;

export default function Layout() {
  const { user, logout, hasAnyPermission } = useAuth();
  const location = useLocation();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  const [drawerOpen, setDrawerOpen] = useState(false);
  const [userAnchor, setUserAnchor] = useState<null | HTMLElement>(null);
  const [changePwOpen, setChangePwOpen] = useState(false);
  const [groupAnchors, setGroupAnchors] = useState<Record<string, HTMLElement | null>>({});

  const visibleItems = NAV_ITEMS.filter(
    (item) => item.permissions.length === 0 || hasAnyPermission(item.permissions),
  );
  const groups = Array.from(new Set(visibleItems.map((i) => i.group)));

  const isActive = (to: string) =>
    to === '/' ? location.pathname === '/' : location.pathname.startsWith(to);

  const isGroupActive = (group: string) =>
    visibleItems.filter((i) => i.group === group).some((i) => isActive(i.to));

  const openGroup = (group: string, el: HTMLElement) =>
    setGroupAnchors((prev) => ({ ...prev, [group]: el }));
  const closeGroup = (group: string) =>
    setGroupAnchors((prev) => ({ ...prev, [group]: null }));

  const userInitials = user?.fullName
    ?.split(' ')
    .map((n) => n[0])
    .slice(0, 2)
    .join('')
    .toUpperCase() ?? '?';

  /* ── Mobile drawer content ───────────────────────── */
  const drawerContent = (
    <Box sx={{ width: DRAWER_WIDTH, height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Drawer brand header */}
      <Box
        sx={{
          background: 'linear-gradient(135deg, #1d4ed8 0%, #4f46e5 100%)',
          px: 2.5, py: 2.5,
          display: 'flex', alignItems: 'center', gap: 1.5,
        }}
      >
        <Box
          sx={{
            width: 36, height: 36, borderRadius: 2,
            background: 'rgba(255,255,255,0.15)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}
        >
          <LocalShippingIcon sx={{ color: 'white', fontSize: 20 }} />
        </Box>
        <Box>
          <Typography variant="h6" fontWeight={800} sx={{ color: 'white', lineHeight: 1.1, letterSpacing: '-0.3px' }}>
            ShipDesk
          </Typography>
          <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.6)', fontSize: 10 }}>
            Enterprise Courier
          </Typography>
        </Box>
      </Box>

      {/* User card in drawer */}
      <Box sx={{ px: 2, py: 2, bgcolor: 'grey.50', borderBottom: '1px solid', borderColor: 'divider' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <Avatar
            sx={{
              width: 38, height: 38, fontSize: 14, fontWeight: 700,
              background: 'linear-gradient(135deg, #1d4ed8, #4f46e5)',
            }}
          >
            {userInitials}
          </Avatar>
          <Box sx={{ minWidth: 0 }}>
            <Typography variant="body2" fontWeight={700} noWrap>{user?.fullName}</Typography>
            <Typography variant="caption" color="text.secondary" noWrap>{user?.companyName}</Typography>
          </Box>
        </Box>
      </Box>

      <Box sx={{ flex: 1, overflowY: 'auto', py: 1 }}>
        {groups.map((group) => {
          const items = visibleItems.filter((i) => i.group === group);
          return (
            <Box key={group}>
              <Typography
                variant="overline"
                sx={{
                  px: 2.5, py: 0.75, display: 'block',
                  color: 'text.disabled', fontSize: 10, fontWeight: 700, letterSpacing: 1.5,
                }}
              >
                {group}
              </Typography>
              {items.map((item) => {
                const active = isActive(item.to);
                return (
                  <ListItemButton
                    key={item.to}
                    component={Link}
                    to={item.to}
                    onClick={() => setDrawerOpen(false)}
                    sx={{
                      mx: 1.5, mb: 0.25, borderRadius: 2, px: 1.5, py: 0.9,
                      bgcolor: active ? 'primary.main' : 'transparent',
                      color: active ? 'white' : 'text.primary',
                      '&:hover': { bgcolor: active ? 'primary.dark' : 'action.hover' },
                    }}
                  >
                    <ListItemIcon sx={{ minWidth: 36, color: active ? 'white' : 'text.secondary' }}>
                      {item.icon}
                    </ListItemIcon>
                    <ListItemText
                      primary={item.label}
                      primaryTypographyProps={{ fontSize: 13.5, fontWeight: active ? 700 : 500 }}
                    />
                    {active && <KeyboardArrowRightIcon sx={{ fontSize: 16, opacity: 0.7 }} />}
                  </ListItemButton>
                );
              })}
            </Box>
          );
        })}
      </Box>

      <Divider />
      <List dense sx={{ py: 1 }}>
        <ListItemButton onClick={() => { setDrawerOpen(false); setChangePwOpen(true); }} sx={{ mx: 1.5, borderRadius: 2 }}>
          <ListItemIcon sx={{ minWidth: 36 }}><LockIcon fontSize="small" /></ListItemIcon>
          <ListItemText primary="Change Password" primaryTypographyProps={{ fontSize: 13 }} />
        </ListItemButton>
        <ListItemButton onClick={() => { setDrawerOpen(false); logout(); }} sx={{ mx: 1.5, borderRadius: 2, color: 'error.main' }}>
          <ListItemIcon sx={{ minWidth: 36, color: 'error.main' }}><LogoutIcon fontSize="small" /></ListItemIcon>
          <ListItemText primary="Sign Out" primaryTypographyProps={{ fontSize: 13 }} />
        </ListItemButton>
      </List>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh', bgcolor: '#f8fafc' }}>
      {/* ── AppBar ──────────────────────────────────────────── */}
      <AppBar
        position="sticky"
        elevation={0}
        sx={{
          background: 'linear-gradient(135deg, #0f172a 0%, #1e3a8a 60%, #312e81 100%)',
          borderBottom: '1px solid rgba(255,255,255,0.08)',
          zIndex: theme.zIndex.drawer + 1,
        }}
      >
        <Toolbar sx={{ gap: 1, minHeight: { xs: 56, md: 60 } }}>
          {isMobile ? (
            <IconButton color="inherit" edge="start" onClick={() => setDrawerOpen(true)} sx={{ mr: 0.5 }}>
              <MenuIcon />
            </IconButton>
          ) : null}

          {/* Logo */}
          <Box
            component={Link}
            to="/"
            sx={{
              display: 'flex', alignItems: 'center', gap: 1,
              textDecoration: 'none', mr: { md: 3 },
            }}
          >
            <Box
              sx={{
                width: 32, height: 32, borderRadius: 1.5,
                background: 'rgba(255,255,255,0.15)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}
            >
              <LocalShippingIcon sx={{ color: 'white', fontSize: 18 }} />
            </Box>
            <Typography
              variant="h6"
              fontWeight={800}
              sx={{ color: 'white', letterSpacing: '-0.3px', display: { xs: 'none', sm: 'block' } }}
            >
              ShipDesk
            </Typography>
          </Box>

          {/* Desktop nav */}
          {!isMobile && (
            <Stack direction="row" spacing={0.5} flex={1} alignItems="center">
              {groups.map((group) => {
                const items = visibleItems.filter((i) => i.group === group);
                const groupActive = isGroupActive(group);

                if (items.length === 1) {
                  return (
                    <Button
                      key={group}
                      component={Link}
                      to={items[0].to}
                      sx={{
                        color: 'rgba(255,255,255,0.85)',
                        textTransform: 'none',
                        fontWeight: groupActive ? 700 : 500,
                        fontSize: 13.5,
                        borderRadius: 2,
                        px: 1.5, py: 0.75,
                        bgcolor: groupActive ? 'rgba(255,255,255,0.12)' : 'transparent',
                        '&:hover': { bgcolor: 'rgba(255,255,255,0.1)', color: 'white' },
                      }}
                    >
                      {items[0].label}
                    </Button>
                  );
                }

                return (
                  <Box key={group}>
                    <Button
                      endIcon={<KeyboardArrowDownIcon sx={{ fontSize: '16px !important' }} />}
                      onClick={(e) => openGroup(group, e.currentTarget)}
                      sx={{
                        color: 'rgba(255,255,255,0.85)',
                        textTransform: 'none',
                        fontWeight: groupActive ? 700 : 500,
                        fontSize: 13.5,
                        borderRadius: 2,
                        px: 1.5, py: 0.75,
                        bgcolor: groupActive ? 'rgba(255,255,255,0.12)' : 'transparent',
                        '&:hover': { bgcolor: 'rgba(255,255,255,0.1)', color: 'white' },
                      }}
                    >
                      {group}
                    </Button>
                    <Menu
                      anchorEl={groupAnchors[group]}
                      open={Boolean(groupAnchors[group])}
                      onClose={() => closeGroup(group)}
                      anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
                      transformOrigin={{ vertical: 'top', horizontal: 'left' }}
                      PaperProps={{
                        sx: {
                          mt: 0.5, borderRadius: 2.5, minWidth: 200,
                          boxShadow: '0 8px 32px rgba(0,0,0,0.15), 0 0 0 1px rgba(0,0,0,0.05)',
                          p: 0.5,
                        },
                      }}
                    >
                      {items.map((item) => {
                        const active = isActive(item.to);
                        return (
                          <MenuItem
                            key={item.to}
                            component={Link}
                            to={item.to}
                            onClick={() => closeGroup(group)}
                            sx={{
                              gap: 1.5, borderRadius: 2, mb: 0.25, px: 1.5, py: 0.9,
                              bgcolor: active ? 'primary.50' : 'transparent',
                              color: active ? 'primary.main' : 'text.primary',
                              fontWeight: active ? 700 : 500,
                              fontSize: 13.5,
                              '& .MuiListItemIcon-root': { color: active ? 'primary.main' : 'text.secondary' },
                            }}
                          >
                            <Box sx={{ color: active ? 'primary.main' : 'text.secondary', display: 'flex' }}>
                              {item.icon}
                            </Box>
                            {item.label}
                            {active && (
                              <Box sx={{ ml: 'auto' }}>
                                <Box sx={{ width: 6, height: 6, borderRadius: '50%', bgcolor: 'primary.main' }} />
                              </Box>
                            )}
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

          {/* Right side user menu */}
          <Tooltip title={user?.fullName ?? ''}>
            <IconButton
              onClick={(e) => setUserAnchor(e.currentTarget)}
              size="small"
              sx={{
                ml: 0.5,
                border: '2px solid rgba(255,255,255,0.2)',
                '&:hover': { border: '2px solid rgba(255,255,255,0.4)' },
              }}
            >
              <Avatar
                sx={{
                  width: 30, height: 30, fontSize: 12, fontWeight: 700,
                  background: 'linear-gradient(135deg, #60a5fa, #818cf8)',
                }}
              >
                {userInitials}
              </Avatar>
            </IconButton>
          </Tooltip>

          <Menu
            anchorEl={userAnchor}
            open={Boolean(userAnchor)}
            onClose={() => setUserAnchor(null)}
            anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
            transformOrigin={{ vertical: 'top', horizontal: 'right' }}
            PaperProps={{
              sx: {
                mt: 1, borderRadius: 2.5, minWidth: 220,
                boxShadow: '0 8px 32px rgba(0,0,0,0.15), 0 0 0 1px rgba(0,0,0,0.05)',
                p: 0.5,
              },
            }}
          >
            {/* User info header */}
            <Box sx={{ px: 2, py: 1.5, mb: 0.5 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                <Avatar
                  sx={{
                    width: 40, height: 40, fontSize: 15, fontWeight: 700,
                    background: 'linear-gradient(135deg, #1d4ed8, #4f46e5)',
                  }}
                >
                  {userInitials}
                </Avatar>
                <Box>
                  <Typography variant="body2" fontWeight={700}>{user?.fullName}</Typography>
                  <Typography variant="caption" color="text.secondary">{user?.companyName}</Typography>
                  <Box sx={{ mt: 0.25 }}>
                    {user?.roles.slice(0, 2).map((r) => (
                      <Chip key={r} label={r} size="small" sx={{ mr: 0.5, height: 16, fontSize: 10 }} />
                    ))}
                  </Box>
                </Box>
              </Box>
            </Box>

            <Divider sx={{ mb: 0.5 }} />

            <MenuItem
              onClick={() => { setUserAnchor(null); setChangePwOpen(true); }}
              sx={{ borderRadius: 2, gap: 1.5, py: 1, px: 1.5, fontSize: 13.5 }}
            >
              <PersonIcon fontSize="small" sx={{ color: 'text.secondary' }} />
              Change Password
            </MenuItem>
            <Divider sx={{ my: 0.5 }} />

            <MenuItem
              onClick={() => { setUserAnchor(null); logout(); }}
              sx={{ borderRadius: 2, gap: 1.5, py: 1, px: 1.5, fontSize: 13.5, color: 'error.main' }}
            >
              <LogoutIcon fontSize="small" />
              Sign Out
            </MenuItem>
          </Menu>

          <ChangePasswordDialog open={changePwOpen} onClose={() => setChangePwOpen(false)} />
        </Toolbar>
      </AppBar>

      {/* Mobile drawer */}
      <Drawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        variant="temporary"
        PaperProps={{ sx: { borderRadius: '0 16px 16px 0', overflow: 'hidden' } }}
      >
        {drawerContent}
      </Drawer>

      {/* ── Page content ───────────────────────────────── */}
      <Box
        component="main"
        sx={{
          flex: 1,
          p: { xs: 2, sm: 3, md: 3 },
          minHeight: 0,
          bgcolor: '#f1f5f9',
        }}
      >
        <Outlet />
      </Box>
    </Box>
  );
}
