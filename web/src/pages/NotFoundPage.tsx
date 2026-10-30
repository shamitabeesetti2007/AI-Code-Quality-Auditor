import React from 'react';
import { Link } from 'react-router-dom';
import { AlertCircle, Home } from 'lucide-react';
import { Card } from '../components/common/Card';
import { Button } from '../components/common/Button';

export const NotFoundPage: React.FC = () => {
  return (
    <div className="py-20 max-w-lg mx-auto text-center space-y-4">
      <Card>
        <div className="py-8 space-y-4">
          <div className="h-14 w-14 rounded-2xl bg-amber-500/10 text-amber-400 border border-amber-500/20 mx-auto flex items-center justify-center shadow-[0_0_20px_-3px_rgba(251,191,36,0.25)]">
            <AlertCircle className="h-7 w-7" />
          </div>
          <h2 className="font-heading text-xl font-bold text-[var(--text-main)]">Route Not Found</h2>
          <p className="text-xs text-[var(--text-muted)] max-w-xs mx-auto leading-relaxed">
            The requested auditor route does not exist or has been relocated.
          </p>
          <div className="pt-2">
            <Link to="/">
              <Button variant="primary" size="sm" icon={<Home className="h-4 w-4" />}>
                Back to Dashboard
              </Button>
            </Link>
          </div>
        </div>
      </Card>
    </div>
  );
};
