"use client";

import { ReactNode, useState } from 'react';
import ComponentCard from '../common/ComponentCard';
import Button from '../ui/button/Button';

interface ButtonProps {
  children: ReactNode; // Button text or content
  size?: "sm" | "md"; // Button size
  variant?: "primary" | "outline"; // Button variant
  startIcon?: ReactNode; // Icon before the text
  endIcon?: ReactNode; // Icon after the text
  onClick?: () => void; // Click handler
  disabled?: boolean; // Disabled state
  className?: string; // Disabled state
}

export default function UseStatePractice() {
  const [number, setNumber] = useState(0);

  return (
    <ComponentCard title="useState()">
      <h1>{number}</h1>
      <div className="flex items-center gap-5">
        <Button size="sm" variant="primary" onClick={() => {
          setNumber(n => n + 1);
          setNumber(n => n + 1);
          setNumber(n => n + 1);
        }}>
          Increment
        </Button>
      </div>
    </ComponentCard>
  )
}

