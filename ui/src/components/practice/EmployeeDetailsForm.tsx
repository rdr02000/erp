"use client";

import { useState } from "react";
import ComponentCard from "@/components/common/ComponentCard";
import Form from "@/components/form/Form";
import Input from "@/components/form/input/InputField";
import Label from "@/components/form/Label";
import Select from "@/components/form/Select";
import Button from "@/components/ui/button/Button";

export interface EmployeeDetails {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  jobTitle: string;
  department: string;
  employmentType: string;
  startDate: string;
}

interface EmployeeDetailsFormProps {
  onSubmit?: (employee: EmployeeDetails) => void;
}

const departments = [
  { value: "engineering", label: "Engineering" },
  { value: "finance", label: "Finance" },
  { value: "human-resources", label: "Human Resources" },
  { value: "marketing", label: "Marketing" },
  { value: "sales", label: "Sales" },
];

const employmentTypes = [
  { value: "full-time", label: "Full-time" },
  { value: "part-time", label: "Part-time" },
  { value: "contract", label: "Contract" },
  { value: "intern", label: "Intern" },
];

export default function EmployeeDetailsForm({
  onSubmit,
}: EmployeeDetailsFormProps) {
  const [submitted, setSubmitted] = useState(false);

  return (
    <ComponentCard
      title="Employee Details"
      desc="Enter the employee's personal and work information."
    >
      <Form
        onSubmit={(event) => {
          const formData = new FormData(event.currentTarget);
          onSubmit?.({
            firstName: String(formData.get("firstName") ?? ""),
            lastName: String(formData.get("lastName") ?? ""),
            email: String(formData.get("email") ?? ""),
            phone: String(formData.get("phone") ?? ""),
            jobTitle: String(formData.get("jobTitle") ?? ""),
            department: String(formData.get("department") ?? ""),
            employmentType: String(formData.get("employmentType") ?? ""),
            startDate: String(formData.get("startDate") ?? ""),
          });
          setSubmitted(true);
        }}
        onChange={() => setSubmitted(false)}
        className="space-y-6"
      >
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <div>
            <Label htmlFor="employee-first-name">First name</Label>
            <Input
              id="employee-first-name"
              name="firstName"
              autoComplete="given-name"
              placeholder="e.g. Jordan"
              required
            />
          </div>
          <div>
            <Label htmlFor="employee-last-name">Last name</Label>
            <Input
              id="employee-last-name"
              name="lastName"
              autoComplete="family-name"
              placeholder="e.g. Lee"
              required
            />
          </div>
          <div>
            <Label htmlFor="employee-email">Work email</Label>
            <Input
              id="employee-email"
              name="email"
              type="email"
              autoComplete="work email"
              placeholder="jordan.lee@example.com"
              required
            />
          </div>
          <div>
            <Label htmlFor="employee-phone">Phone number</Label>
            <Input
              id="employee-phone"
              name="phone"
              type="tel"
              autoComplete="tel"
              placeholder="+1 (555) 000-0000"
            />
          </div>
          <div>
            <Label htmlFor="employee-job-title">Job title</Label>
            <Input
              id="employee-job-title"
              name="jobTitle"
              placeholder="e.g. Product Designer"
              required
            />
          </div>
          <div>
            <Label htmlFor="employee-department">Department</Label>
            <Select
              id="employee-department"
              name="department"
              options={departments}
              placeholder="Select a department"
              onChange={() => setSubmitted(false)}
              required
            />
          </div>
          <div>
            <Label htmlFor="employee-type">Employment type</Label>
            <Select
              id="employee-type"
              name="employmentType"
              options={employmentTypes}
              placeholder="Select employment type"
              onChange={() => setSubmitted(false)}
              required
            />
          </div>
          <div>
            <Label htmlFor="employee-start-date">Start date</Label>
            <Input
              id="employee-start-date"
              name="startDate"
              type="date"
              required
            />
          </div>
        </div>

        <div className="flex flex-col gap-4 border-t border-gray-100 pt-5 dark:border-gray-800 sm:flex-row sm:items-center sm:justify-between">
          <p
            aria-live="polite"
            className="text-sm text-success-600 dark:text-success-400"
          >
            {submitted ? "Employee details submitted." : ""}
          </p>
          <Button size="sm">Save Employee</Button>
        </div>
      </Form>
    </ComponentCard>
  );
}
