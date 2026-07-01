import { useState } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import {
  AppBar,
  Avatar,
  Box,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  ListSubheader,
  Menu,
  MenuItem,
  Toolbar,
  Typography,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import DashboardIcon from '@mui/icons-material/Dashboard';
import PeopleIcon from '@mui/icons-material/People';
import SecurityIcon from '@mui/icons-material/Security';
import RuleIcon from '@mui/icons-material/Rule';
import ContactsIcon from '@mui/icons-material/Contacts';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import AssessmentIcon from '@mui/icons-material/Assessment';
import LogoutIcon from '@mui/icons-material/Logout';
import { useAuth } from '../context/AuthContext';

const DRAWER_WIDTH = 248;

interface NavItem {
  label: string;
  to: string;
  icon: JSX.Element;
  permissions: string[];
  group: string;
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', to: '/', icon: <DashboardIcon />, permissions: [], group: 'General' },
  { label: 'Users', to: '/admin/users', icon: <PeopleIcon />, permissions: ['ADMIN_VIEW'], group: 'Admin' },
  { label: 'Roles & Permissions', to: '/admin/roles', icon: <SecurityIcon />, permissions: ['ADMIN_VIEW'], group: 'Admin' },
  { label: 'Approval Routing', to: '/admin/approval-routing', icon: <RuleIcon />, permissions: ['ADMIN_VIEW'], group: 'Admin' },
  { label: 'Parties', to: '/master/parties', icon: <ContactsIcon />, permissions: ['MASTER_VIEW'], group: 'Master' },
  { label: 'Bookings', to: '/bookings', icon: <LocalShippingIcon />, permissions: ['BOOKING_VIEW'], group: 'Booking' },
  { label: 'Reports', to: '/reports', icon: <AssessmentIcon />, permissions: ['REPORTS_VIEW'], group: 'Reports' },
];

export default function Layout() {
  const { user, logout, hasAnyPermission } = useAuth();
  const location = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const visibleItems = NAV_ITEMS.filter(
    (item) => item.permissions.length === 0 || hasAnyPermission(item.permissions)
  );

  const groups = Array.from(new Set(visibleItems.map((i) => i.group)));

  const drawer = (
    <Box>
      <Toolbar>
        <LocalShippingIcon sx={{ mr: 1, color: 'primary.main' }} />
        <Typography variant="h6" noWrap fontWeight={700}>
          CourierApp
        </Typography>
      </Toolbar>
      <Divider />
      {groups.map((group) => (
        <List
          key={group}
          subheader={<ListSubheader sx={{ bgcolor: 'transparent' }}>{group}</ListSubheader>}
        >
          {visibleItems
            .filter((i) => i.group === group)
            .map((item) => {
              const selected =
                item.to === '/' ? location.pathname === '/' : location.pathname.startsWith(item.to);
              return (
                <ListItemButton
                  key={item.to}
                  component={Link}
                  to={item.to}
                  selected={selected}
                  onClick={() => setMobileOpen(false)}
                >
                  <ListItemIcon>{item.icon}</ListItemIcon>
                  <ListItemText primary={item.label} />
                </ListItemButton>
              );
            })}
        </List>
      ))}
    </Box>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar
        position="fixed"
        sx={{ width: { md: `calc(100% - ${DRAWER_WIDTH}px)` }, ml: { md: `${DRAWER_WIDTH}px` } }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            edge="start"
            onClick={() => setMobileOpen(!mobileOpen)}
            sx={{ mr: 2, display: { md: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" noWrap sx={{ flexGrow: 1 }}>
            Courier Booking System
          </Typography>
          <Typography variant="body2" sx={{ mr: 1, display: { xs: 'none', sm: 'block' } }}>
            {user?.fullName}
          </Typography>
          <IconButton color="inherit" onClick={(e) => setAnchorEl(e.currentTarget)}>
            <Avatar sx={{ width: 32, height: 32, bgcolor: 'secondary.main' }}>
              {user?.fullName?.charAt(0).toUpperCase()}
            </Avatar>
          </IconButton>
          <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
            <MenuItem disabled>{user?.username} ({user?.roles.join(', ')})</MenuItem>
            <Divider />
            <MenuItem onClick={logout}>
              <ListItemIcon>
                <LogoutIcon fontSize="small" />
              </ListItemIcon>
              Logout
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Box component="nav" sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }}>
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: 'block', md: 'none' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: DRAWER_WIDTH },
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          open
          sx={{
            display: { xs: 'none', md: 'block' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: DRAWER_WIDTH },
          }}
        >
          {drawer}
        </Drawer>
      </Box>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          minHeight: '100vh',
        }}
      >
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
}
