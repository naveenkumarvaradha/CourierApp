import { useEffect, useState } from 'react';
import { Card, CardActionArea, CardContent, Grid, Stack, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import PeopleIcon from '@mui/icons-material/People';
import ContactsIcon from '@mui/icons-material/Contacts';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import AssessmentIcon from '@mui/icons-material/Assessment';
import { useAuth } from '../context/AuthContext';
import { reportApi } from '../api/endpoints';
import type { ReportSummary } from '../types';

export default function DashboardPage() {
  const { user, hasAnyPermission } = useAuth();
  const navigate = useNavigate();
  const [summary, setSummary] = useState<ReportSummary | null>(null);

  useEffect(() => {
    if (hasAnyPermission(['REPORTS_VIEW'])) {
      reportApi.summary({ granularity: 'monthly' }).then(setSummary).catch(() => undefined);
    }
  }, [hasAnyPermission]);

  const tiles = [
    { label: 'Users', icon: <PeopleIcon fontSize="large" />, to: '/admin/users', perm: ['ADMIN_VIEW'] },
    { label: 'Parties', icon: <ContactsIcon fontSize="large" />, to: '/master/parties', perm: ['MASTER_VIEW'] },
    { label: 'Bookings', icon: <LocalShippingIcon fontSize="large" />, to: '/bookings', perm: ['BOOKING_VIEW'] },
    { label: 'Reports', icon: <AssessmentIcon fontSize="large" />, to: '/reports', perm: ['REPORTS_VIEW'] },
  ].filter((t) => hasAnyPermission(t.perm));

  return (
    <Stack spacing={3}>
      <Typography variant="h4" fontWeight={600}>
        Welcome, {user?.fullName}
      </Typography>

      {summary && (
        <Grid container spacing={2}>
          <StatCard label="Bookings (This Month)" value={summary.totalBookings} />
          <StatCard label="Total Charges" value={`₹ ${summary.totalCharges}`} />
          <StatCard label="Total Freight" value={`₹ ${summary.totalFreight}`} />
          <StatCard
            label="Pending Approval"
            value={summary.countByStatus['PENDING_APPROVAL'] ?? 0}
          />
        </Grid>
      )}

      <Typography variant="h6">Quick Access</Typography>
      <Grid container spacing={2}>
        {tiles.map((tile) => (
          <Grid item xs={12} sm={6} md={3} key={tile.to}>
            <Card>
              <CardActionArea onClick={() => navigate(tile.to)}>
                <CardContent sx={{ textAlign: 'center', py: 4 }}>
                  <Stack alignItems="center" spacing={1} color="primary.main">
                    {tile.icon}
                    <Typography variant="subtitle1" color="text.primary">
                      {tile.label}
                    </Typography>
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

function StatCard({ label, value }: { label: string; value: string | number }) {
  return (
    <Grid item xs={12} sm={6} md={3}>
      <Card>
        <CardContent>
          <Typography variant="body2" color="text.secondary">
            {label}
          </Typography>
          <Typography variant="h5" fontWeight={600}>
            {value}
          </Typography>
        </CardContent>
      </Card>
    </Grid>
  );
}
